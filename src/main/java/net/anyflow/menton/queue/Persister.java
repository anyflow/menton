/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.List;

/**
 * @author anyflow
 */
public interface Persister<Message> {

	/**
	 * @param target
	 *            message list to persist
	 * @return true when all messages persisted otherwise false.
	 */
	boolean persist(List<Message> target);

	/**
	 * @return max processing message count in a cycle
	 */
	int maxProcessingSize();
}
