package net.anyflow.menton.http;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

public class DefaultHttpServerHandler extends SimpleChannelUpstreamHandler {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultHttpServerHandler.class);

	private RequestHandler requestHandler;

	public DefaultHttpServerHandler(String requestHandlerPackageRoot) throws DefaultException {
		Configurator.setRequestHandlerPackageRoot(requestHandlerPackageRoot);
	}

	public DefaultHttpServerHandler(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		final HttpRequest request = (HttpRequest)e.getMessage();
		final HttpResponse response = createDefaultResponse(request);

		// TODO Replace multithreadings with typical proxy code.
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<String> future = executorService.submit(new Callable<String>() {

			@Override
			public String call() {
				logger.info(request.getUri().toString() + " requested.");

				try {
					String path = (new URI(request.getUri())).getPath();

					return requestHandler != null ? handleRequestByMethodTypeHandler(e, request, response, path)
							: handleRequestByClassTypeHandler(e, request, response, path);
				}
				catch(URISyntaxException e1) {
					response.setStatus(HttpResponseStatus.NOT_FOUND);
					logger.info("unexcepted URI : {}", request.getUri().toString(), e);
					return null;
				}
				catch(InstantiationException e) {
					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					logger.error("Generating new request handler instance failed.", e);
					return null;
				}
				catch(IllegalAccessException e) {
					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					logger.error("Generating new request handler instance failed.", e);
					return null;
				}
				catch(Exception e) {
					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					logger.error("Unknown exception was thrown", e);
					return null;
				}
			}

			/**
			 * @param e
			 * @param request
			 * @param response
			 * @param path
			 * @return
			 * @throws DefaultException
			 * @throws IllegalAccessException
			 * @throws InvocationTargetException
			 */
			private String handleRequestByMethodTypeHandler(final MessageEvent e, final HttpRequest request,
					final HttpResponse response, String path) throws DefaultException, IllegalAccessException,
					InvocationTargetException {

				requestHandler.initialize(request, response, e);
				Method handler = requestHandler.findHandler(path, request.getMethod().toString());

				if(handler == null) {
					response.setStatus(HttpResponseStatus.NOT_FOUND);
					logger.info("unexcepted URI : {}", request.getUri().toString(), e);

					return "Failed to find the request handler.";
				}
				else {
					return handler.invoke(requestHandler, (Object[])null).toString();
				}
			}

			/**
			 * @param e
			 * @param request
			 * @param response
			 * @param path
			 * @return
			 * @throws DefaultException
			 * @throws InstantiationException
			 * @throws IllegalAccessException
			 */
			private String handleRequestByClassTypeHandler(final MessageEvent e, final HttpRequest request,
					final HttpResponse response, String path) throws DefaultException, InstantiationException,
					IllegalAccessException {
				Class<? extends RequestHandler> handlerClass = RequestHandler
						.find(path, request.getMethod().toString());

				if(handlerClass == null) {
					response.setStatus(HttpResponseStatus.NOT_FOUND);
					logger.info("unexcepted URI : {}", request.getUri().toString(), e);

					return "Failed to find the request handler.";
				}
				else {
					RequestHandler handler = handlerClass.newInstance();

					handler.initialize(request, response, e);

					return handler.call();
				}
			}
		});

		String content = future.get();
		executorService.shutdownNow();

		writeResponse(response, normalize(content), HttpHeaders.isKeepAlive(request), e);
	}

	private String normalize(String content) {
		// TODO normalize content.. but for what?

		return content;
	}

	private HttpResponse createDefaultResponse(HttpRequest request) {
		// Build the response object.
		HttpResponse ret = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

		ret.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");

		// resolve cross domain issue
		ret.setHeader("Access-Control-Allow-Origin", "*");
		ret.setHeader("Access-Control-Allow-Methods", "POST, GET");
		ret.setHeader("Access-Control-Allow-Headers", "X-PINGARUNER");
		ret.setHeader("Access-Control-Max-Age", "1728000");

		String cookieString = request.getHeader(HttpHeaders.Names.COOKIE);
		if(cookieString == null || cookieString.isEmpty()) { return ret; }

		CookieDecoder cookieDecoder = new CookieDecoder();
		Set<Cookie> cookies = cookieDecoder.decode(cookieString);
		if(cookies.isEmpty()) { return ret; }

		// Reset the cookies if necessary.
		CookieEncoder cookieEncoder = new CookieEncoder(true);
		for(Cookie cookie : cookies) {
			cookieEncoder.addCookie(cookie);
			ret.addHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
		}

		return ret;
	}

	private void writeResponse(HttpResponse response, String content, boolean isKeepAlive, MessageEvent e) {

		response.setContent(ChannelBuffers.copiedBuffer(content == null ? "" : content, CharsetUtil.UTF_8));

		if(isKeepAlive) {
			// Add 'Content-Length' header only for a keep-alive connection.
			response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes());
			// Add keep alive header as per:
			// -
			// http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
			response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		// Write the response.
		ChannelFuture future = e.getChannel().write(response);

		// Close the non-keep-alive connection after the write operation is
		// done.
		if(!isKeepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
}