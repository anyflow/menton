/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * 
 * @author Park Hyunjeong
 * @param <Item>
 *            processing target type
 */
public class ProcessingQueue<Item> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProcessingQueue.class);
	private static final int MAX_ACTIVE_THREAD_SIZE = Runtime.getRuntime().availableProcessors() * 4;

	private final PriorityBlockingQueue<Item> queue;
	private final Processor<Item> processor;
	private final int maxProcessingSize;
	
	public ProcessingQueue(Processor<Item> processor) {
		this(processor, null);
	}

	public ProcessingQueue(Processor<Item> processor, Comparator<Item> comparator) {

		this.processor = processor;

		maxProcessingSize = processor.maxProcessingSize() > 0 ? processor.maxProcessingSize() : MAX_ACTIVE_THREAD_SIZE;
		
		queue = new PriorityBlockingQueue<Item>(maxProcessingSize, comparator);
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

		for(int i = 0; i < maxProcessingSize; ++i) {

			executor.submit(new Runnable() {

				@Override
				public void run() {

					ArrayList<Item> targets = new ArrayList<Item>();

					while(true) {
						targets.clear();

						try {
							targets.add(queue.take()); // wait until item inserted newly.

							while(true) {
								Item item = queue.poll();
								if(item == null) {
									break;
								}

								targets.add(item);

								if(targets.size() >= maxProcessingSize) {
									break;
								}
							}
						}
						catch(InterruptedException e) {
							logger.error("waiting interrupted unintentionally.", e);
							continue;
						}
						
						processor.process(targets);
						processor.processingCompleted(targets);
					}
				}
			});
		}

		logger.info("ProcessingQueue started with max processing size : {}", maxProcessingSize);
	}
}