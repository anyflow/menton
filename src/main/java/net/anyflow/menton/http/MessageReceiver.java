/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author anyflow
 */
public interface MessageReceiver {

	/**
	 * Will be called on message received.
	 * 
	 * @param request
	 * @param response
	 */
	void messageReceived(FullHttpRequest request, FullHttpResponse response);
}