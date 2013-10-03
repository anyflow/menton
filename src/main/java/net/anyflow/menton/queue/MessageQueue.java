/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author anyflow
 */
public class MessageQueue<Element> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageQueue.class);

	private final PriorityBlockingQueue<Element> guestsForPersisting;
	private final PriorityBlockingQueue<Element> guestsForTransfering;
	private final Persister<Element> persister;
	private final Transferer<Element> transferer;

	public MessageQueue(Persister<Element> persister, Transferer<Element> transferer) {
		this(persister, transferer, null, null);
	}

	public MessageQueue(Persister<Element> persister, Transferer<Element> transferer, Comparator<Element> peristComparator,
			Comparator<Element> transferComparator) {

		this.persister = persister;
		this.transferer = transferer;

		guestsForPersisting = new PriorityBlockingQueue<Element>(persister.maxProcessingSize(), peristComparator);
		guestsForTransfering = new PriorityBlockingQueue<Element>(transferer.maxProcessingSize(), transferComparator);
	}

	public void start() {
		Executors.newSingleThreadExecutor().execute(
				new MessagePump(new PersisterHandler(persister), guestsForPersisting, persister.maxProcessingSize()));
		Executors.newSingleThreadExecutor().execute(
				new MessagePump(new TransfererHandler(transferer), guestsForTransfering, transferer.maxProcessingSize()));

		logger.info("Message Queue started with max processing size persister : {}, transferer : {}.", persister.maxProcessingSize(),
				transferer.maxProcessingSize());
	}

	/**
	 * Add notification to process...
	 * 
	 * @param notification
	 */
	public void queueForPersisting(Element element) {

		if(guestsForPersisting.contains(element)) { return; }
		if(guestsForPersisting.offer(element) == false) { return; }

		synchronized(guestsForPersisting) {
			guestsForPersisting.notify();
		}
	}

	public void queueForTransfering(List<Element> elements) {

		ArrayList<Element> list = new ArrayList<Element>();

		for(Element item : elements) {
			if(guestsForTransfering.contains(item)) {
				continue;
			}

			list.add(item);
		}

		guestsForTransfering.addAll(list);

		synchronized(guestsForTransfering) {
			guestsForTransfering.notify();
		}
	}

	private class MessagePump implements Runnable {

		private final PriorityBlockingQueue<Element> guests;
		private final int maxProcessingSize;
		private final MessageHandler<Element> handler;

		public MessagePump(MessageHandler<Element> handler, PriorityBlockingQueue<Element> guests, int maxProcessingSize) {
			this.handler = handler;
			this.guests = guests;
			this.maxProcessingSize = maxProcessingSize;
		}

		@Override
		public void run() {
			while(true) {
				if(guests.size() <= 0) {
					try {
						synchronized(guests) {
							guests.wait();
						}
					}
					catch(InterruptedException e) {
						logger.error("waiting interrupted unintentionally.", e);
						continue;
					}
				}

				handler.handle(pollTasks());
			}
		}

		private List<Element> pollTasks() {

			ArrayList<Element> tasks = new ArrayList<Element>();

			while(true) {
				if(guests.size() <= 0) {
					break;
				}
				if(tasks.size() >= maxProcessingSize) {
					break;
				}

				tasks.add(guests.poll());
			}

			logger.debug("polled task count : {}", tasks.size());
			return tasks;
		}
	}

	private interface MessageHandler<Message> {

		/**
		 * @param target
		 */
		void handle(List<Message> target);
	}

	/**
	 * @author anyflow
	 */
	private class PersisterHandler implements MessageHandler<Element> {

		private final Persister<Element> persister;
		private final ExecutorService executor;

		public PersisterHandler(Persister<Element> persister) {
			this.persister = persister;
			executor = Executors.newCachedThreadPool();
		}

		/*
		 * (non-Javadoc)
		 * @see anyflow.pushService.pushCenter.MessageQueue.Writer#write(java.util .List)
		 */
		@Override
		public void handle(final List<Element> target) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					logger.debug("Persisting started. target size : {}", target.size());

					if(persister.persist(target)) {
						MessageQueue.this.queueForTransfering(target);
					}
					else {
						guestsForPersisting.addAll(target);

						synchronized(guestsForPersisting) {
							guestsForPersisting.notify();
						}
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
		private final ExecutorService executor;

		public TransfererHandler(Transferer<Element> transferer) {
			this.transferer = transferer;
			this.executor = Executors.newCachedThreadPool();
		}

		/*
		 * (non-Javadoc)
		 * @see anyflow.pushService.pushCenter.MessageQueue.Writer#write(java.util .List)
		 */
		@Override
		public void handle(List<Element> target) {
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
				MessageQueue.this.queueForTransfering(fails);
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