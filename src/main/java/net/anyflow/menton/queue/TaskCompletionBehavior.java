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
	 * @param taskName completed task name
	 * @param safeToShutdown true if shutdown is safe
	 */
	void taskCompleted(String taskName, boolean safeToShutdown);
}