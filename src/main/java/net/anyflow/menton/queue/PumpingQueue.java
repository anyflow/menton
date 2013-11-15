/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * Queue with a message pump works in in a dedicated thread, which processes Items via {@link net.anyflow.menton.queue.Processor}.
 * <ul>
 * <li>The queue supports priority based processing(via {@link java.util.PriorityQueue}).
 * <li>The queue provides thread-safety.
 * <li>The queue provides synchronization type(Blocking / Non-blocking processing).
 * <li>The queue provides completion event via {@link net.anyflow.menton.queue.Processor#processingCompleted(List)}.
 * <li>The queue provides processing task count in runtime.
 * </ul>
 * 
 * @author Park Hyunjeong
 * @param <Item>
 *            processing target type
 */
public class PumpingQueue<Item> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PumpingQueue.class);
	private static final int defaultProcessingThreadSize = Runtime.getRuntime().availableProcessors() * 4;

	private final PriorityBlockingQueue<Item> queue;
	private final Processor<Item> processor;
	private final Synchronization synchronization;
	private final List<Future<?>> tasks;
	private final int processingThreadSize;

	/**
	 * <p>
	 * Processing type
	 * 
	 * @author Park Hyunjeong
	 */
	public enum Synchronization {
		/**
		 * The queue processes the next loop tasks after the current loop tasks completed.
		 */
		BLOCKING,

		/**
		 * The queue processes the next loop tasks right after the current loop tasks submited.
		 */
		NONBLOCKING
	}

	public PumpingQueue(Processor<Item> processor) {
		this(processor, Synchronization.BLOCKING, null, defaultProcessingThreadSize);
	}

	public PumpingQueue(Processor<Item> processor, Synchronization synchronization) {

		this(processor, synchronization, null, defaultProcessingThreadSize);
	}

	public PumpingQueue(Processor<Item> processor, Synchronization synchronization, Comparator<Item> comparator, int processingThreadSize) {

		this.processor = processor;
		this.synchronization = synchronization;

		this.processingThreadSize = processingThreadSize;

		queue = new PriorityBlockingQueue<Item>(processor.maxProcessingSize(), comparator);
		tasks = new ArrayList<Future<?>>();
	}

	/**
	 * @return current running task count include the tasks in the queue.
	 */
	public int runningTaskCount() {
		int ret = queue.size();

		synchronized(queue) {
			for(Future<?> task : tasks) {
				if(task.isDone() == false) {
					++ret;
				}
			}
		}

		return ret;
	}

	/**
	 * @return queue size
	 */
	public int size() {
		return queue.size();
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

	/**
	 * Start message pump in the queue.
	 */
	public void start() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				ExecutorService executor = Executors.newFixedThreadPool(processingThreadSize);

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
					}

					for(int i = 0; i < processor.maxProcessingSize(); ++i) {
						targets.add(queue.poll()); // PumpingQueue doesn't have a remove operation, so poll() returns non-null item definitely.

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

					tasks.add(future);

					// task clean up.
					List<Future<?>> removes = new ArrayList<Future<?>>();
					for(Future<?> task : tasks) {
						if(task.isDone()) {
							removes.add(task);
						}
					}
					tasks.removeAll(removes);

					switch(synchronization) {

					case BLOCKING:
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

					case NONBLOCKING:
					default:
						break;
					}
				}
			}
		});

		logger.info("PumpingQueue started with max processing size : {}", processor.maxProcessingSize());
	}
}