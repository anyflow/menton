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

import net.anyflow.menton.Configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

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
		if(content == null) {
			content = "";
		}

		content().writeBytes(content.getBytes(CharsetUtil.UTF_8));
		logger.debug(content().toString(CharsetUtil.UTF_8));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append("\r\n");
		buf.append("HTTP Status: " + this.getStatus()).append("\r\n");
		buf.append("Version: " + this.getProtocolVersion()).append("\r\n");
		buf.append("Response Headers: ").append("\r\n");

		if(!this.headers().isEmpty()) {
			for(String name : this.headers().names()) {
				for(String value : this.headers().getAll(name)) {
					buf.append("   ").append(name).append(" = ").append(value).append("\r\n");
				}
			}
		}

		String content = this.content().toString(CharsetUtil.UTF_8);

		int size = Ints.tryParse(Configurator.instance().getProperty("menton.logging.httpResponseContentSize", "100"));

		if(size < 0) {
			buf.append("Content:\r\n   ").append(content);
		}
		else {
			int index = content.length() < size ? content.length() : size - 1;
			buf.append("The first " + size + " character(s) of response content:\r\n   ").append(content.substring(0, index));
		}

		return buf.toString();
	}
}