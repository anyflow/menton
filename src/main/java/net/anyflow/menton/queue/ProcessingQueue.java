/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

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
public class ProcessingQueue<Item extends Comparable<Item>> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProcessingQueue.class);
	private static final int DEFAULT_PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors() * 4;

	private final PriorityBlockingQueue<Item> queue;
	private final Processor<Item> processor;
	private final int processorCount;

	public ProcessingQueue(Processor<Item> processor) {
		this(processor, DEFAULT_PROCESSOR_COUNT);
	}

	public ProcessingQueue(Processor<Item> processor, int processorCount) {
		this.processor = processor;
		this.processorCount = processorCount;
		this.queue = new PriorityBlockingQueue<Item>(processor.maxProcessingSize());
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
	}

	/**
	 * Start message pump in the queue.
	 */
	public void start() {
		ExecutorService executor = Executors.newCachedThreadPool();

		for(int i = 0; i < processorCount; ++i) {

			String name = this.getClass().getSimpleName() + "[" + processor.getClass().getSimpleName() + "] - " + i;

			executor.submit(new Consumer(name));
		}

		logger.info("{}[{}] started.\r\nProcessor count: {}\r\nmax processing size: {}\r\n", new Object[] { this.getClass().getSimpleName(),
				processor.getClass().getSimpleName(), processorCount, processor.maxProcessingSize() });
	}

	class Consumer implements Runnable {

		private final String name;

		public Consumer(String name) {
			this.name = name;
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
					targets.add(queue.take()); // wait until item inserted newly.
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