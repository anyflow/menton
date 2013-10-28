/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

/**
 * @author anyflow
 */
public class HttpRequest extends DefaultFullHttpRequest {

	private Channel channel;

	/**
	 * @param channel
	 * @param httpVersion
	 * @param method
	 * @param uri
	 * @param content
	 * @param headers
	 * @param decoderResult
	 */
	public HttpRequest(Channel channel, FullHttpRequest fullHttpRequest) {
		super(fullHttpRequest.getProtocolVersion(), fullHttpRequest.getMethod(), fullHttpRequest.getUri(), fullHttpRequest.content().copy());

		this.channel = channel;
		this.headers().set(fullHttpRequest.headers());
		this.trailingHeaders().set(fullHttpRequest.trailingHeaders());
		this.setDecoderResult(fullHttpRequest.getDecoderResult());
	}

	public String host() {
		return this.headers().get(HttpHeaders.Names.HOST);
	}

	public Map<String, List<String>> parameters() {
		String queryStringParam = null;

		if(getMethod().equals(HttpMethod.GET)) {
			queryStringParam = getUri();
		}
		else if(getMethod().equals(HttpMethod.POST)) {
			String dummy = "/dummy?";
			queryStringParam = dummy + content().toString(CharsetUtil.UTF_8);
		}
		else {
			throw new UnsupportedOperationException("only GET/POST http methods are supported.");
		}

		return (new QueryStringDecoder(queryStringParam)).parameters();
	}

	/**
	 * Get single parameter. In case of multiple values, the method returns the first.
	 * 
	 * @param name
	 *            parameter name.
	 * @return The first value of the parameter name. If it does not exist, it returns an empty string.
	 */
	public String parameter(String name) {
		Map<String, List<String>> params = parameters();

		if(params.containsKey(name) == false || params.get(name).size() <= 0) { return ""; }

		return parameters().get(name).get(0);
	}

	public Channel channel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}