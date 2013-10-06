/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author anyflow
 */
public class PumpingQueue<Item> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PumpingQueue.class);

	private final PriorityQueue<Item> queue;
	private final Processor<Item> processor;
	private final Synchronization synchronization;

	public enum Synchronization {
		ONE_BY_ONE, SIMULTANEOUS
	}

	public PumpingQueue(Processor<Item> processor) {
		this(processor, Synchronization.ONE_BY_ONE, null);
	}

	public PumpingQueue(Processor<Item> processor, Synchronization synchronization) {
		this(processor, synchronization, null);
	}

	public PumpingQueue(Processor<Item> processor, Synchronization synchronization, Comparator<Item> comparator) {

		this.processor = processor;
		this.synchronization = synchronization;

		queue = new PriorityQueue<Item>(processor.maxProcessingSize(), comparator);
	}

	public void enqueue(Item item) {

		if(queue.contains(item)) { return; }
		if(queue.offer(item) == false) { return; }

		synchronized(queue) {
			queue.notify();
		}
	}

	public void enqueue(List<Item> items) {
		for(Item element : items) {
			if(queue.contains(element)) {
				continue;
			}

			if(queue.offer(element) == false) {
				continue;
			}
		}

		if(queue.size() <= 0) { return; }

		synchronized(queue) {
			queue.notify();
		}
	}

	public void start() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				ExecutorService executor = Executors.newCachedThreadPool();

				while(true) {
					final ArrayList<Item> targets = new ArrayList<Item>(); // the list should be created per loop.

					synchronized(queue) {
						try {
							// blocks until item appears..
							if(queue.size() <= 0) {
								queue.wait();
							}
						}
						catch(InterruptedException e) {
							logger.error("waiting interrupted unintentionally.", e);
							continue;
						}

						if(queue.size() > 0) {
							for(int i = 0; i < processor.maxProcessingSize(); ++i) {
								targets.add(queue.poll());

								if(queue.size() == 0) {
									break;
								}
							}

							Future<?> future = executor.submit(new Runnable() {

								@Override
								public void run() {
									processor.process(targets);
									processor.processingCompleted(targets);
								}
							});

							switch(synchronization) {

							case ONE_BY_ONE:
								try {
									future.get(processor.prcessingTimeout(), TimeUnit.MILLISECONDS);
								}
								catch(InterruptedException e) {
									enqueue(targets);
									logger.error("Processing " + targets.toString() + " failed, so enqueued again.", e);
								}
								catch(ExecutionException e) {
									enqueue(targets);
									logger.error("Processing " + targets.toString() + " failed, so enqueued again.", e);
								}
								catch(TimeoutException e) {
									enqueue(targets);
									logger.error("Processing " + targets.toString() + " failed, so enqueued again.", e);
								}
								break;

							case SIMULTANEOUS:
							default:
								break;
							}
						}
					}
				}
			}
		});

		logger.info("PumpingQueue started with max processing size : {}", processor.maxProcessingSize());
	}
}