/**
 * 
 */
package net.anyflow.menton.http;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import net.anyflow.menton.exception.DefaultException;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;
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
	private final Map<String, String> cookies;
	private final Map<String, String> parameters;
	private final Map<String, String> headers;

	public HttpClient() {
		httpMethod = HttpMethod.GET;

		cookies = new HashMap<String, String>();
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
	 * @param isSynchronousMode
	 * @param receiver
	 * @return if isSynchronousMode is true and the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(boolean isSynchronousMode, final MessageReceiver receiver) throws DefaultException, UnsupportedEncodingException {
		return request(isSynchronousMode, receiver, "utf-8");
	}

	/**
	 * request.
	 * 
	 * @param isSynchronousMode
	 * @param receiver
	 * @param queryEncodingCharset
	 *            query encoding charset. if it is null, no encoding will be applied.
	 * @return if isSynchronousMode is true and the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(boolean isSynchronousMode, final MessageReceiver receiver, String queryEncodingCharset) throws DefaultException,
			UnsupportedEncodingException {

		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();

		// TODO support HTTPS
		if(!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
			logger.error("Only HTTP(S) is supported.");
			return null;
		}

		final ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		final HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri.getRawPath());

		setHeaders(request);
		addParameters(request, queryEncodingCharset);
		debugRequest(request);

		final HttpClientHandler clientHandler = new HttpClientHandler(receiver, request);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = org.jboss.netty.channel.Channels.pipeline();

				pipeline.addLast("codec", new HttpClientCodec());
				pipeline.addLast("chunkAggregator",new HttpChunkAggregator(1048576));
				pipeline.addLast("inflateer", new HttpContentDecompressor());
				pipeline.addLast("handler", clientHandler);

				return pipeline;
			}
		});

		ChannelFuture bootstrapFuture = bootstrap.connect(new InetSocketAddress(uri.getHost(), getPort()));

		final Channel channel = bootstrapFuture.awaitUninterruptibly().getChannel();
		if(!bootstrapFuture.isSuccess()) {
			logger.error("connection failed.", bootstrapFuture.getCause());
			bootstrap.releaseExternalResources();
			return null;
		}

		// Register resources releasing handler
		channel.getCloseFuture().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				new Thread(new Runnable() {

					@Override
					public void run() {
						channel.close().awaitUninterruptibly();
						bootstrap.releaseExternalResources();
						logger.debug("HttpClient resources released.");
					}
				}).start();
			}
		});

		channel.write(request);

		if(isSynchronousMode == false) { return null; }

		try {
			if(channel.getCloseFuture().isDone() == false) {
				channel.getCloseFuture().await();
			}

			return clientHandler.getResponse();
		}
		catch(InterruptedException e) {
			logger.error("waiting interrupted.", e);
			return null;
		}
	}

	/**
	 * @param request
	 */
	private void debugRequest(final HttpRequest request) {
		logger.debug("[request] URI : {}", request.getUri());
		logger.debug("[request] CONTENT : {}", request.getContent().toString(CharsetUtil.UTF_8));
		logger.debug("[request] HTTPMETHOD : {}", request.getMethod().toString());
		if(!request.getHeaderNames().isEmpty()) {
			for(String name : request.getHeaderNames()) {
				for(String value : request.getHeaders(name)) {
					logger.debug("[request] HEADER : " + name + " = " + value);
				}
			}
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

		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		request.setHeader(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
		for(Entry<String, String> item : headers.entrySet()) {
			request.setHeader(item.getKey(), item.getValue());
		}

		CookieEncoder httpCookieEncoder = new CookieEncoder(false);

		for(Entry<String, String> item : cookies.entrySet()) {
			httpCookieEncoder.addCookie(item.getKey(), item.getValue());
		}
		request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
	}

	/**
	 * @param request
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	private void addParameters(HttpRequest request, String queryEncodingCharset) throws DefaultException, UnsupportedEncodingException {

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
			request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");

			if(parameters.size() > 0) {
				String paramsString = getParametersString(queryEncodingCharset);

				Charset charset = queryEncodingCharset == null ? Charset.defaultCharset() : Charset.forName(queryEncodingCharset);

				ChannelBuffer cb = ChannelBuffers.copiedBuffer(paramsString, charset);

				request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, cb.readableBytes());
				request.setContent(cb);
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
	public void setCookie(String key, String value) {
		cookies.put(key, value);
	}

	/**
	 * @return
	 */
	public Map<String, String> getCookies() {
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
	public class HttpClientHandler extends SimpleChannelUpstreamHandler {

		private final MessageReceiver receiver;
		private final HttpRequest request;
		private HttpResponse response;

		public HttpClientHandler(MessageReceiver receiver, HttpRequest request) {
			this.receiver = receiver;
			this.request = request;
		}

		public HttpResponse getResponse() {
			return response;
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

			if(e.getMessage() instanceof HttpResponse == false) { return; }

			response = (HttpResponse)e.getMessage();

			debugResponse();

			e.getChannel().close();

			if(receiver != null) {
				receiver.messageReceived(request, response);
			}
		}

		/**
		 * 
		 */
		private void debugResponse() {

			logger.debug("[response] STATUS : " + response.getStatus());
			logger.debug("[response] VERSION : " + response.getProtocolVersion());

			if(!response.getHeaderNames().isEmpty()) {
				for(String name : response.getHeaderNames()) {
					for(String value : response.getHeaders(name)) {
						logger.debug("[response] HEADER : " + name + " = " + value);
					}
				}
			}

			ChannelBuffer content = response.getContent();
			if(content.readable()) {
				logger.debug("[response] CONTENT {");
				logger.debug(content.toString(CharsetUtil.UTF_8));
				logger.debug("[response] } END OF CONTENT");
			}
		}

	}
}