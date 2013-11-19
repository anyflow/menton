/**
 * 
 */
package net.anyflow.menton.queue;

/**
 * @author Park Hyunjeong
 */
public interface TaskCompletionBehavior {

	/**
	 * called after task completed
	 * 
	 * @param safeToShutdown
	 *            true if shutdown is safe
	 */
	void taskCompleted(boolean safeToShutdown);
}