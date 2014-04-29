/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.List;

/**
 * Item processor for {@link net.anyflow.menton.queue.PumpingQueue}.
 * 
 * @author Park Hyunjeong
 * @param <Item>
 *            processing target class
 */
public interface Processor<Item> {

	/**
	 * @return maximum processing task count in one loop.
	 */
	int maxProcessingSize();

	/**
	 * @param items
	 *            items processing target list
	 */
	void process(List<Item> items);

	/**
	 * @return processing timeout in millisecond
	 */
	int prcessingTimeout();
}