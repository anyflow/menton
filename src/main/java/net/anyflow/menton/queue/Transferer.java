/**
 * 
 */
package net.anyflow.menton.queue;

/**
 * @author anyflow
 */
public interface Transferer<Message> {

	boolean transfer(Message target);

	/**
	 * @return max processing message count in a cycle
	 */
	int maxProcessingSize();

	/**
	 * @return transfer task timeout in millisecond
	 */
	int timeout();
}
