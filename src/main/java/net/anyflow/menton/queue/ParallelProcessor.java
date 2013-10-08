/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * The abstract class inherits {@link net.anyflow.menton.queue.Processor} which uses a thread per <code>Item</code>.
 * 
 * @author Park Hyunjeong
 * @param <Item>
 */
public abstract class ParallelProcessor<Item> implements Processor<Item> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParallelProcessor.class);

	class Task implements Callable<Boolean> {

		Item item;

		public Task(Item item) {
			this.item = item;
		}

		public Item item() {
			return item;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Boolean call() throws Exception {
			return process(item);
		}
	}

	/**
	 * @param item
	 *            item to process
	 * @return true the processing finished successfully.
	 */
	public abstract Boolean process(Item item);

	/**
	 * The event handler which is called when {@link #process(List)} finished with false return value.
	 * 
	 * @param item
	 *            the item processed.
	 */
	public abstract void processingFailedWithReturn(Item item);

	/**
	 * The event handler which is called when {@link #process(List)} interrupted with an InterruptedException
	 * 
	 * @param item
	 *            the item processed.
	 * @param e
	 *            the cause the handler called.
	 */
	public abstract void processingFailedWith(Item item, InterruptedException e);

	/**
	 * The event handler which is called when {@link #process(List)} interrupted with an ExecutionException
	 * 
	 * @param item
	 *            the item processed.
	 * @param e
	 *            the cause the handler called.
	 */
	public abstract void processingFailedWith(Item item, ExecutionException e);

	/**
	 * The event handler which is called when {@link #process(List)} interrupted with an TimeoutException
	 * 
	 * @param item
	 *            the item processed.
	 * @param e
	 *            the cause the handler called.
	 */
	public abstract void processingFailedWith(Item item, TimeoutException e);

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.queue.Processor#process(java.util.List)
	 */
	@Override
	public void process(List<Item> items) {

		Map<Task, Future<Boolean>> tasks = new HashMap<Task, Future<Boolean>>();

		ExecutorService executor = Executors.newCachedThreadPool();
		for(Item item : items) {
			Task task = new Task(item);

			tasks.put(task, executor.submit(task));
		}

		for(Task task : tasks.keySet()) {
			try {
				if(tasks.get(task).get(prcessingTimeout(), TimeUnit.MILLISECONDS) == false) {

					processingFailedWithReturn(task.item());

					logger.error("Processing " + task.item().toString() + " failed, so enqueued again.");
				}
			}
			catch(InterruptedException e) {
				processingFailedWith(task.item(), e);
				logger.error("Processing " + task.item().toString() + " failed, so enqueued again.", e);
			}
			catch(ExecutionException e) {
				processingFailedWith(task.item(), e);
				logger.error("Processing " + task.item().toString() + " failed, so enqueued again.", e);
			}
			catch(TimeoutException e) {
				processingFailedWith(task.item(), e);
				logger.error("Processing " + task.item().toString() + " failed, so enqueued again.", e);
			}
		}
	}
}