/**
 * 
 */
package net.anyflow.menton.queue;

import java.util.List;

/**
 * @author anyflow
 */
public interface Persister<Message> {

	boolean persist(List<Message> target);

	int getMaxProcessingSize();
}
