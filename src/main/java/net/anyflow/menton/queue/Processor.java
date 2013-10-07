/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.List;

/**
 * @author anyflow
 */
public interface Processor<Item> {

	/**
	 * @return maximum processing task count in one loop.
	 */
	int maxProcessingSize();

	/**
	 * @param items
	 *            items to process.
	 */
	void process(List<Item> items);

	/**
	 * @return processing timeout in millisecond
	 */
	int prcessingTimeout();

	/**
	 * The event handler called in the worker thread when processing completed.
	 * 
	 * @param items
	 *            items which are completed.
	 */
	void processingCompleted(List<Item> items);
}