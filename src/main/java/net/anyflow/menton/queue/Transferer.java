/**
 * 
 */
package net.anyflow.menton.queue;

/**
 * @author anyflow
 */
public interface Transferer<Message> {

	boolean transfer(Message target);

	int getMaxProcessingSize();

	/**
	 * @return transfer task timeout in millisecond
	 */
	int timeout();
}
