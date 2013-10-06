/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.anyflow.menton.exception.DefaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpClient {

	static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private URI uri;

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	private HttpMethod httpMethod;
	private final Map<String, Cookie> cookies;
	private final Map<String, List<String>> parameters;
	private final Map<String, String> headers;

	public HttpClient() {
		httpMethod = HttpMethod.GET;

		cookies = new HashMap<String, Cookie>();
		parameters = new HashMap<String, List<String>>();
		headers = new HashMap<String, String>();
	}

	public HttpClient(URI uri) {
		this();

		this.uri = uri;
	}

	/**
	 * Request with encoding utf-8
	 * 
	 * @param receiver
	 * @return if receiver is not null and the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(final MessageReceiver receiver) throws IllegalArgumentException, UnsupportedEncodingException {
		return request(receiver, "utf-8");
	}

	/**
	 * request.
	 * 
	 * @param receiver
	 * @param queryEncodingCharset
	 *            query encoding charset. if it is null, no encoding will be applied.
	 * @return if receiver is not null the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(final MessageReceiver receiver, String queryEncodingCharset) throws IllegalArgumentException,
			UnsupportedEncodingException {

		Thread.currentThread().setName("client/main");

		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();

		boolean ssl = false;

		// TODO support HTTPS
		if(!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
			logger.error("Only HTTP(S) is supported.");
			return null;
		}

		final EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("client"));

		HttpRequest request = new HttpRequest(null, new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri.getRawPath()));

		setHeaders(request);
		addParameters(request, queryEncodingCharset);

		if(logger.isDebugEnabled()) {
			logger.debug("[request] URI : {}", request.getUri());
			logger.debug("[request] CONTENT : {}", request.content().toString(CharsetUtil.UTF_8));
			logger.debug("[request] HTTPMETHOD : {}", request.getMethod().toString());

			for(String name : request.headers().names()) {
				logger.debug("[request] HEADER : " + name + " = " + request.headers().get(name));
			}
		}

		HttpClientHandler clientHandler = new HttpClientHandler(receiver, request);

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioSocketChannel.class).handler(new ClientChannelInitializer(clientHandler, ssl));

		try {
			Channel channel = bootstrap.connect(uri.getHost(), port()).sync().channel();
			channel.writeAndFlush(request);

			if(receiver == null) {
				channel.closeFuture().sync();
				group.shutdownGracefully();

				return clientHandler.httpResponse();
			}
			else {
				channel.closeFuture().addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						group.shutdownGracefully();
					}
				});

				return null;
			}
		}
		catch(InterruptedException e) {

			group.shutdownGracefully();
			logger.error(e.getMessage(), e);

			return null;
		}
	}

	private int port() {
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();

		int port = uri.getPort();
		if(port == -1) {
			if(scheme.equalsIgnoreCase("http")) {
				return 80;
			}
			else if(scheme.equalsIgnoreCase("https")) { return 443; }
		}

		return port;
	}

	private void setHeaders(HttpRequest request) {

		String host = uri.getHost() == null ? "localhost" : uri.getHost();

		for(Entry<String, String> item : headers.entrySet()) {
			request.headers().set(item.getKey(), item.getValue());
		}

		if(httpMethod == HttpMethod.POST && headers.containsKey(HttpHeaders.Names.CONTENT_TYPE) == false) {
			request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
		}

		request.headers().set(HttpHeaders.Names.HOST, host);
		request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP + ", " + HttpHeaders.Values.DEFLATE);
		request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");

		request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookies.values()));
	}

	/**
	 * @param request
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	private void addParameters(FullHttpRequest request, String queryEncodingCharset) throws IllegalArgumentException, UnsupportedEncodingException {

		String address = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath();

		if(httpMethod == HttpMethod.GET) {
			String query = uri.getQuery();

			if(query != null && query.length() > 0) {

				String[] tokens = query.split("=|&");
				for(int i = 0; i < tokens.length; ++i) {
					if(i % 2 == 0) {
						continue;
					}

					if(parameters.containsKey(tokens[i - 1])) {
						if(tokens[i - 1].endsWith("[]") == false) { throw new IllegalArgumentException("A parameter is duplicated : " + tokens[i - 1]); }
						parameters.get(tokens[i - 1]).add(tokens[i]);
					}
					else {
						List<String> value = new ArrayList<String>();
						value.add(tokens[i]);
						parameters.put(tokens[i - 1], value);
					}
				}
			}

			if(parameters.size() > 0) {
				address += "?" + makeQueryString(queryEncodingCharset);
			}
		}
		else if(httpMethod == HttpMethod.POST && parameters.size() > 0) {
			String paramsString = makeQueryString(queryEncodingCharset);

			Charset charset = queryEncodingCharset == null ? Charset.defaultCharset() : Charset.forName(queryEncodingCharset);

			ByteBuf content = Unpooled.copiedBuffer(paramsString, charset);

			request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes());
			request.content().clear();
			request.content().writeBytes(content);
		}
		else {
			throw new IllegalArgumentException("only GET/POST methods are supported.");
		}

		request.setUri(address);
	}

	/**
	 * @param httpMethod
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void setCookie(String name, String value) {
		cookies.put(name, new DefaultCookie(name, value));
	}

	/**
	 * @return
	 */
	public Map<String, Cookie> cookies() {
		return cookies;
	}

	/**
	 * Add an array parameter values. The method replace the former value if the parameter is present.
	 * 
	 * @param name
	 * @param values
	 * @throws IllegalArgumentException
	 *             is thrown in case of array parameter name(which is ended with '[]') passed.
	 */
	public void addParameter(String name, List<String> values) throws IllegalArgumentException {
		if(name.endsWith("[]") == false) { throw new IllegalArgumentException("For non-array parameter, use addParameter(String, String)."); }

		parameters.remove(name);

		parameters.put(name, values);
	}

	/**
	 * Add a non-array parameter value. The method replace the former value if the parameter is present.
	 * 
	 * @param name
	 * @param value
	 * @throws IllegalArgumentException
	 *             is thrown in case of array parameter name(which is ended with '[]') passed.
	 */
	public void addParameter(String name, String value) throws IllegalArgumentException {
		if(name.endsWith("[]")) { throw new IllegalArgumentException("For array parameter, use addParameter(String, List<String>)."); }

		parameters.remove(name);

		List<String> values = new ArrayList<String>();
		values.add(value);
		parameters.put(name, values);
	}

	/**
	 * @return parameter list.
	 */
	public Map<String, List<String>> parameters() {
		return parameters;
	}

	/**
	 * Add a header value. The method replace the former value if the header is present.
	 * 
	 * @param key
	 * @param value
	 */
	public void addHeader(String key, String value) {
		headers.remove(key);
		headers.put(key, value);
	}

	/**
	 * @return headers
	 */
	public Map<String, String> headers() {
		return headers;
	}

	private String makeQueryString(String queryEncodingCharset) throws UnsupportedEncodingException {
		if(parameters.size() <= 0) { return ""; }

		StringBuilder query = new StringBuilder();
		String innerValue = null;

		for(Map.Entry<String, List<String>> item : parameters.entrySet()) {

			for(String value : item.getValue()) {
				innerValue = queryEncodingCharset != null ? java.net.URLEncoder.encode(value, queryEncodingCharset) : value;

				query = query.append(item.getKey()).append("=").append(innerValue).append("&");
			}
		}

		query = query.deleteCharAt(query.length() - 1);
		return query.toString();
	}
}