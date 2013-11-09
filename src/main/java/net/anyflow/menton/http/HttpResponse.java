/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpResponse extends DefaultFullHttpResponse {

	private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);

	private final Channel channel;

	public static HttpResponse createServerDefault(Channel channel, String requestCookie) {

		HttpResponse ret = new HttpResponse(channel, HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());

		ret.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");

		// Set request cookies
		if(requestCookie != null) {
			Set<Cookie> cookies = CookieDecoder.decode(requestCookie);
			if(!cookies.isEmpty()) {
				// Reset the cookies if necessary.
				for(Cookie cookie : cookies) {
					ret.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
				}
			}
		}

		return ret;
	}

	public static HttpResponse createFrom(FullHttpResponse source, Channel channel) {
		HttpResponse ret = new HttpResponse(channel, source.getProtocolVersion(), source.getStatus(), source.content().copy());

		ret.headers().set(source.headers());
		ret.trailingHeaders().set(source.trailingHeaders());

		return ret;
	}

	/**
	 * @param version
	 * @param status
	 */
	private HttpResponse(Channel channel, HttpVersion version, HttpResponseStatus status, ByteBuf content) {
		super(version, status, content);

		this.channel = channel;
	}

	public Channel channel() {
		return channel;
	}

	public void setContent(String content) {
		if(content == null) { content = ""; }
		
		content().writeBytes(content.getBytes(CharsetUtil.UTF_8));
		logger.debug(content().toString(CharsetUtil.UTF_8));
	}
}