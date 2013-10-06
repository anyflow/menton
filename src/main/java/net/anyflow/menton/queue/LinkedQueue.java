/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author anyflow
 */
public class LinkedQueue<Element> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LinkedQueue.class);

	private final List<PriorityQueue<Element>> guests;

	private final PriorityQueue<Element> guestsForPersisting;
	private final PriorityQueue<Element> guestsForTransfering;
	private final Persister<Element> persister;
	private final Transferer<Element> transferer;

	public LinkedQueue(Persister<Element> persister, Transferer<Element> transferer) {
		this(persister, transferer, null, null);
	}

	public LinkedQueue(Persister<Element> persister, Transferer<Element> transferer, Comparator<Element> peristComparator,
			Comparator<Element> transferComparator) {

		this.persister = persister;
		this.transferer = transferer;

		guestsForPersisting = new PriorityQueue<Element>(persister.maxProcessingSize(), peristComparator);
		guestsForTransfering = new PriorityQueue<Element>(transferer.maxProcessingSize(), transferComparator);

		guests = new ArrayList<PriorityQueue<Element>>();

		guests.add(guestsForPersisting);
		guests.add(guestsForTransfering);
	}

	public void start() {
		Executors.newSingleThreadExecutor().execute(
				new MessagePump(new PersisterHandler(persister), guestsForPersisting, persister.maxProcessingSize()));
		Executors.newSingleThreadExecutor().execute(
				new MessagePump(new TransfererHandler(transferer), guestsForTransfering, transferer.maxProcessingSize()));

		logger.info("LinkedQueue started with max processing size persister : {}, transferer : {}.", persister.maxProcessingSize(),
				transferer.maxProcessingSize());
	}

	public void enqueue(List<Element> elements, int queueIndex) {
		enqueue(elements, guests.get(queueIndex));
	}

	public void enqueue(Element element, int queueIndex) {
		enqueue(element, guests.get(queueIndex));
	}

	/**
	 * The method is thread-safe.
	 * 
	 * @param elements
	 *            to persist process
	 */
	public void queueForPersisting(List<Element> elements) {
		enqueue(elements, guestsForPersisting);
	}

	/**
	 * The method is thread-safe.
	 * 
	 * @param elements
	 *            to transfer process
	 */
	public void queueForTransfering(List<Element> elements) {
		enqueue(elements, guestsForTransfering);
	}

	/**
	 * The method is thread-safe.
	 * 
	 * @param element
	 *            to persist process
	 */
	public void queueForPersisting(Element element) {
		enqueue(element, guestsForPersisting);
	}

	/**
	 * The method is thread-safe.
	 * 
	 * @param element
	 *            to persist process
	 */
	public void queueForTransfering(Element element) {
		enqueue(element, guestsForTransfering);
	}

	private void enqueue(Element element, Queue<Element> target) {

		if(target.contains(element)) { return; }
		if(target.offer(element) == false) { return; }

		synchronized(target) {
			target.notify();
		}
	}

	private void enqueue(List<Element> elements, Queue<Element> target) {
		for(Element element : elements) {
			if(target.contains(element)) {
				continue;
			}

			if(target.offer(element) == false) {
				continue;
			}
		}

		if(target.size() <= 0) { return; }

		synchronized(target) {
			target.notify();
		}
	}

	private class MessagePump implements Runnable {

		private final Queue<Element> guests;
		private final int maxProcessingSize;
		private final MessageHandler<Element> handler;
		private final ExecutorService handleExecutor;

		public MessagePump(MessageHandler<Element> handler, Queue<Element> guests, int maxProcessingSize) {
			this.handler = handler;
			this.guests = guests;
			this.maxProcessingSize = maxProcessingSize;
			this.handleExecutor = Executors.newCachedThreadPool();
		}

		@Override
		public void run() {
			ArrayList<Element> messages = null;

			while(true) {
				messages = new ArrayList<Element>(); // the list should be created per loop.

				synchronized(guests) {
					try {
						// blocks until guests appears..
						if(guests.size() <= 0) {
							guests.wait();
						}
					}
					catch(InterruptedException e) {
						logger.error("waiting interrupted unintentionally.", e);
						continue;
					}

					if(guests.size() > 0) {
						for(int i = 0; i < maxProcessingSize; ++i) {
							messages.add(guests.poll());

							if(guests.size() == 0) {
								break;
							}
						}

						handler.handle(handleExecutor, messages);
					}
				}
			}
		}
	}

	private interface MessageHandler<Message> {

		/**
		 * @param target
		 */
		void handle(ExecutorService executor, List<Message> target);
	}

	/**
	 * @author anyflow
	 */
	private class PersisterHandler implements MessageHandler<Element> {

		private final Persister<Element> persister;

		public PersisterHandler(Persister<Element> persister) {
			this.persister = persister;
		}

		/*
		 * (non-Javadoc)
		 * @see anyflow.pushService.pushCenter.MessageQueue.Writer#write(java.util .List)
		 */
		@Override
		public void handle(ExecutorService executor, final List<Element> target) {
			executor.execute(new Runnable() {

				@Override
				public void run() {

					logger.debug("Persisting started. target size : {}", target.size());

					if(persister.persist(target)) {
						LinkedQueue.this.queueForTransfering(target);
					}
					else {
						LinkedQueue.this.queueForPersisting(target);
					}
				}
			});
		}
	}

	/**
	 * @author anyflow
	 */
	private class TransfererHandler implements MessageHandler<Element> {

		private final Transferer<Element> transferer;

		public TransfererHandler(Transferer<Element> transferer) {
			this.transferer = transferer;
		}

		/*
		 * (non-Javadoc)
		 * @see anyflow.pushService.pushCenter.MessageQueue.Writer#write(java.util .List)
		 */
		@Override
		public void handle(ExecutorService executor, List<Element> target) {
			logger.debug("Transfering started. target size : {}", target.size());

			List<SimpleEntry<Task, Future<Boolean>>> tasks = new ArrayList<SimpleEntry<Task, Future<Boolean>>>();

			for(Element item : target) {
				tasks.add(new SimpleEntry<Task, Future<Boolean>>(new Task(item, transferer), null));
			}

			for(SimpleEntry<Task, Future<Boolean>> item : tasks) {
				Future<Boolean> future = executor.submit(item.getKey());

				item.setValue(future);
			}

			ArrayList<Element> fails = new ArrayList<Element>();

			for(SimpleEntry<Task, Future<Boolean>> item : tasks) {
				try {
					if(item.getValue().get(transferer.timeout(), TimeUnit.MILLISECONDS) == false) {
						logger.error("Transfering({}) failed. The task returns false.", item.getKey().getTarget().toString());
						fails.add(item.getKey().getTarget());
					}
				}
				catch(InterruptedException e) {
					logger.error("Transfering({}) failed.", item.getKey().getTarget().toString(), e);
					fails.add(item.getKey().getTarget());
				}
				catch(ExecutionException e) {
					logger.error("Transfering({}) failed.", item.getKey().getTarget().toString(), e);
					fails.add(item.getKey().getTarget());
				}
				catch(TimeoutException e) {
					logger.error("Transfering({}) failed.", item.getKey().getTarget().toString(), e);
					fails.add(item.getKey().getTarget());
				}
			}

			if(fails.size() > 0) {
				LinkedQueue.this.queueForTransfering(fails);
			}

			logger.info("Transfering finished. queue size : {}", guestsForTransfering.size());
		}

		private class Task implements Callable<Boolean> {

			private final Element target;
			private final Transferer<Element> writer;

			public Task(Element target, Transferer<Element> writer) {
				this.target = target;
				this.writer = writer;
			}

			public Element getTarget() {
				return target;
			}

			@Override
			public Boolean call() throws Exception {
				return writer.transfer(target);
			}
		}
	}
}