/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

/**
 * <p>
 * Queue with dedicated consumer threads, which processes Items via {@link net.anyflow.menton.queue.Processor}.
 * <ul>
 * <li>The queue supports priority based processing(via {@link java.util.PriorityQueue}).
 * <li>The queue provides thread-safety.
 * <li>The queue provides completion event via {@link net.anyflow.menton.queue.Processor#processingCompleted(List)}.
 * </ul>
 * 
 * @author Park Hyunjeong
 * @param <Item>
 *            processing target type
 */
public class ProcessingQueue<Item extends Comparable<Item>> implements TaskCompletionInformer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProcessingQueue.class);
	private static final int DEFAULT_PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors() * 2;

	private final PriorityBlockingQueue<Item> queue;
	private final Processor<Item> processor;
	private List<Consumer> consumers;
	private List<TaskCompletionListener> taskCompletionListeners;

	private final int processorCount;
	private boolean shutdownSignaled;
	private Integer maxSize;

	public ProcessingQueue(Processor<Item> processor) {
		this(processor, DEFAULT_PROCESSOR_COUNT);
	}

	public ProcessingQueue(Processor<Item> processor, int processorCount) {
		this.processor = processor;
		this.processorCount = processorCount;
		this.queue = new PriorityBlockingQueue<Item>(processor.maxProcessingSize());
		this.consumers = new ArrayList<Consumer>();
		this.taskCompletionListeners = new ArrayList<TaskCompletionListener>();
		this.shutdownSignaled = false;
		this.maxSize = null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#register(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void register(TaskCompletionListener taskCompletionListener) {
		this.taskCompletionListeners.add(taskCompletionListener);
	}

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#deregister(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void deregister(TaskCompletionListener taskCompletionListener) {
		this.taskCompletionListeners.remove(taskCompletionListener);
	}

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#inform()
	 */
	@Override
	public void inform() {
		if(isTaskCompleted() == false) { return; }

		for(TaskCompletionListener taskCompletionListener : taskCompletionListeners) {
			taskCompletionListener.taskCompleted(this, !shutdownSignaled);
		}
	}

	public void signalShutdown() {
		this.shutdownSignaled = true;

		inform();
	}

	public boolean isShutdownSignaled() {
		return this.shutdownSignaled;
	}

	public Processor<Item> processor() {
		return processor;
	}

	/**
	 * @return queue size
	 */
	public int size() {
		return queue.size();
	}

	public void setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
	}

	public void enqueue(Item item) throws EnqueueLimitedException {

		if(shutdownSignaled) { throw new EnqueueLimitedException("Shutdown of the queue signaled."); }

		if(queue.contains(item)) { return; }

		if(maxSize != null && size() >= maxSize) { throw new EnqueueLimitedException("The queue is full."); }

		queue.offer(item);
	}

	public void enqueue(List<Item> items) throws EnqueueLimitedException {
		if(shutdownSignaled) { throw new EnqueueLimitedException("Shutdown of the queue signaled."); }

		for(Item element : items) {
			if(queue.contains(element)) {
				continue;
			}

			if(maxSize != null && size() >= maxSize) { throw new EnqueueLimitedException("The queue is full."); }

			queue.offer(element);
		}
	}

	/**
	 * Start message pump in the queue.
	 */
	public void start() {
		ExecutorService executor = Executors.newCachedThreadPool();

		for(int i = 0; i < processorCount; ++i) {
			String name = this.getClass().getSimpleName() + "[" + processor.getClass().getSimpleName() + "] - " + i;
			Consumer consumer = new Consumer(name);

			consumers.add(consumer);
			executor.submit(consumer);
		}

		logger.info("{}[{}] started[Processor count: {} / max processing size: {}]", new Object[] { this.getClass().getSimpleName(),
				processor.getClass().getSimpleName(), processorCount, processor.maxProcessingSize() });
	}

	public boolean isTaskCompleted() {
		if(size() > 0) { return false; }

		for(Consumer consumer : consumers) {
			if(consumer.isSuspended() == false) { return false; }
		}

		return true;
	}

	class Consumer implements Runnable {

		private final String name;
		private boolean isSuspended;

		public Consumer(String name) {
			this.name = name;
			isSuspended = true;
		}

		public boolean isSuspended() {
			return isSuspended;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			Thread.currentThread().setName(name);

			ArrayList<Item> targets = new ArrayList<Item>();

			while(true) {
				targets.clear();

				try {
					isSuspended = true;

					inform();

					targets.add(queue.take()); // wait until item inserted newly.
					isSuspended = false;
				}
				catch(InterruptedException e) {
					logger.error("waiting interrupted unintentionally.", e);
					continue;
				}

				while(true) {
					Item item = queue.poll();
					if(item == null) {
						break;
					}

					targets.add(item);

					if(targets.size() >= processor.maxProcessingSize()) {
						break;
					}
				}

				processor.process(targets);
				processor.processingCompleted(targets);
			}
		}
	}
}