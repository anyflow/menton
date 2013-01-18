/**
 * 
 */
package anyflow.engine.network.http;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * @author anyflow
 *
 */
public interface MessageReceiver {

	/**
	 * Will be called on message received.
	 * @param request
	 * @param response
	 */
	void messageReceived(HttpRequest request, HttpResponse response);
}
