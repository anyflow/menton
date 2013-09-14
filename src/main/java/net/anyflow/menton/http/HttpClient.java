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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.anyflow.menton.exception.DefaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpClient {

	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

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
	private final Map<String, String> parameters;
	private final Map<String, String> headers;

	public HttpClient() {
		httpMethod = HttpMethod.GET;

		cookies = new HashMap<String, Cookie>();
		parameters = new HashMap<String, String>();
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
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	public FullHttpResponse request(final MessageReceiver receiver) throws DefaultException, UnsupportedEncodingException {
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
	public FullHttpResponse request(final MessageReceiver receiver, String queryEncodingCharset) throws DefaultException,
			UnsupportedEncodingException {

		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();

		final boolean ssl = false;

		// TODO support HTTPS
		if(!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
			logger.error("Only HTTP(S) is supported.");
			return null;
		}

		final EventLoopGroup group = new NioEventLoopGroup();

		try {
			final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri.getRawPath());

			setHeaders(request);
			addParameters(request, queryEncodingCharset);
			debugRequest(request);

			final HttpClientHandler clientHandler = new HttpClientHandler(receiver, request);

			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {

					ChannelPipeline p = ch.pipeline();

					p.addLast("log", new LoggingHandler(LogLevel.INFO));

					if(ssl) {
						// TODO
						// SSL related..

						// p.addLast("ssl", new SslHandler(engine));
					}

					p.addLast("codec", new HttpClientCodec());
					p.addLast("chunkAggregator", new HttpObjectAggregator(1048576));
					p.addLast("inflater", new HttpContentDecompressor());
					p.addLast("handler", clientHandler);
				}

			});

			Channel channel = bootstrap.connect(uri.getHost(), getPort()).sync().channel();
			channel.writeAndFlush(request);

			if(receiver == null) {
				channel.closeFuture().sync();
				group.shutdownGracefully();

				return clientHandler.getResponse();
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

	/**
	 * @param request
	 */
	private void debugRequest(final FullHttpRequest request) {
		logger.debug("[request] URI : {}", request.getUri());
		logger.debug("[request] CONTENT : {}", request.content().toString(CharsetUtil.UTF_8));
		logger.debug("[request] HTTPMETHOD : {}", request.getMethod().toString());

		if(request.headers().isEmpty() == true) { return; }

		for(String name : request.headers().names()) {
			logger.debug("[request] HEADER : " + name + " = " + request.headers().get(name));
		}
	}

	private int getPort() {
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

		request.headers().set(HttpHeaders.Names.HOST, host);
		request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
		for(Entry<String, String> item : headers.entrySet()) {
			request.headers().set(item.getKey(), item.getValue());
		}

		request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookies.values()));
	}

	/**
	 * @param request
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	private void addParameters(FullHttpRequest request, String queryEncodingCharset) throws DefaultException, UnsupportedEncodingException {

		String address = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath();

		if(httpMethod == HttpMethod.GET) {
			String query = uri.getQuery();

			if(query != null && query.length() > 0) {

				String[] tokens = query.split("=|&");
				for(int i = 0; i < tokens.length; ++i) {
					if(i % 2 == 0) {
						continue;
					}

					parameters.put(tokens[i - 1], tokens[i]);
				}
			}

			if(parameters.size() > 0) {
				address += "?" + getParametersString(queryEncodingCharset);
			}
		}
		else if(httpMethod == HttpMethod.POST) {
			request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");

			if(parameters.size() > 0) {
				String paramsString = getParametersString(queryEncodingCharset);

				Charset charset = queryEncodingCharset == null ? Charset.defaultCharset() : Charset.forName(queryEncodingCharset);

				ByteBuf content = Unpooled.copiedBuffer(paramsString, charset);

				request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes());
				request.content().clear();
				request.content().writeBytes(content);
			}
		}
		else {
			throw new DefaultException("only GET/POST methods are supported.");
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
	public Map<String, Cookie> getCookies() {
		return cookies;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	/**
	 * @return
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void addHeader(String key, String value) {
		headers.put(key, value);
	}

	/**
	 * @return
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	private String getParametersString(String queryEncodingCharset) throws UnsupportedEncodingException {
		if(parameters.size() <= 0) { return ""; }

		StringBuilder query = new StringBuilder();
		String value = null;

		for(Map.Entry<String, String> item : parameters.entrySet()) {

			value = queryEncodingCharset != null ? java.net.URLEncoder.encode(item.getValue(), queryEncodingCharset) : item.getValue();

			query = query.append(item.getKey()).append("=").append(value).append("&");
		}

		query = query.deleteCharAt(query.length() - 1);
		return query.toString();
	}

	/**
	 * @author anyflow
	 */
	public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

		private final MessageReceiver receiver;
		private final FullHttpRequest request;
		private FullHttpResponse response;

		public HttpClientHandler(MessageReceiver receiver, FullHttpRequest request) {
			this.receiver = receiver;
			this.request = request;
		}

		public FullHttpResponse getResponse() {
			return response;
		}

		/*
		 * (non-Javadoc)
		 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty .channel.ChannelHandlerContext, java.lang.Object)
		 */
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
			this.response = msg;

			logger.debug("[response] STATUS : " + response.getStatus());
			logger.debug("[response] VERSION : " + response.getProtocolVersion());

			if(!response.headers().isEmpty()) {
				for(String name : response.headers().names()) {
					for(String value : response.headers().getAll(name)) {
						logger.debug("[response] HEADER : " + name + " = " + value);
					}
				}
			}

			if(response.content().isReadable()) {
				logger.debug("[response] CONTENT {");
				logger.debug(response.content().toString(CharsetUtil.UTF_8));
				logger.debug("[response] } END OF CONTENT");
			}

			if(receiver != null) {
				receiver.messageReceived(request, response);
			}

			ctx.channel().close();
		}
	}
}
