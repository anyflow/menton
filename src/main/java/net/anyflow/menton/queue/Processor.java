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
	 * @return
	 */
	int maxProcessingSize();

	/**
	 * @param executor
	 * @param items
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
	 */
	void processingCompleted(List<Item> items);
}