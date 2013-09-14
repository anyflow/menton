/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

/**
 * @author anyflow
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpServerHandler.class);

	private FullHttpRequest request;
	private RequestHandler requestHandler;
	private String requestHandlerPackageRoot;

	public HttpServerHandler(String requestHandlerPackageRoot) {
		this.requestHandlerPackageRoot = requestHandlerPackageRoot;
	}

	public HttpServerHandler(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	/*
	 * (non-Javadoc)
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

		if(HttpHeaders.is100ContinueExpected(request)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			return;
		}

		request = msg;

		debugRequest(request);

		logger.info(request.getUri().toString() + " requested.");

		FullHttpResponse response = createDefaultResponse(request.headers().get(HttpHeaders.Names.COOKIE));
		if(Configurator.instance().getProperty("allow_cross_domain").equalsIgnoreCase("yes")) {
			response.headers().add("Access-Control-Allow-Origin", "*");
			response.headers().add("Access-Control-Allow-Methods", "POST, GET");
			response.headers().add("Access-Control-Allow-Headers", "X-PINGARUNER");
			response.headers().add("Access-Control-Max-Age", "1728000");
		}

		try {
			String path = (new URI(request.getUri())).getPath();

			String content = requestHandler != null ? handleMethodTypeHandler(ctx.channel(), request, response, path) : handleClassTypeHandler(
					ctx.channel(), request, response, path);

			response.content().setBytes(0, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		}
		catch(URISyntaxException e) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());
		}
		catch(InstantiationException e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Generating new request handler instance failed.", e);
		}
		catch(IllegalAccessException e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Generating new request handler instance failed.", e);
		}
		catch(Exception e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Unknown exception was thrown", e);
		}

		boolean keepAlive = request.headers().get(HttpHeaders.Names.CONNECTION) == HttpHeaders.Values.KEEP_ALIVE;
		if(keepAlive) {
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		ctx.writeAndFlush(response);
		if(keepAlive == false) {
			ctx.close().addListener(ChannelFutureListener.CLOSE);
		}
	}

	private String handleClassTypeHandler(Channel channel, FullHttpRequest request, FullHttpResponse response, String requestedPath)
			throws DefaultException, InstantiationException, IllegalAccessException {

		Class<? extends RequestHandler> handlerClass = RequestHandler.find(requestedPath, request.getMethod().toString(),
				this.requestHandlerPackageRoot);

		if(handlerClass == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());

			return "Failed to find the request handler.";
		}
		else {
			RequestHandler handler = handlerClass.newInstance();

			handler.initialize(channel, request, response);

			return handler.call();
		}
	}

	private String handleMethodTypeHandler(Channel channel, FullHttpRequest request, FullHttpResponse response, String requestedPath)
			throws DefaultException, IllegalAccessException, InvocationTargetException {

		requestHandler.initialize(channel, request, response);
		Method handler = requestHandler.findHandler(requestedPath, request.getMethod().toString());

		if(handler == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());

			return "Failed to find the request handler.";
		}
		else {
			return handler.invoke(requestHandler, (Object[])null).toString();
		}
	}

	private FullHttpResponse createDefaultResponse(String requestCookie) {

		FullHttpResponse ret = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());

		ret.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");

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

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelReadComplete();
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	private void debugRequest(HttpRequest request) {
		StringBuilder buf = new StringBuilder();

		buf.setLength(0);
		buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
		buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
		buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

		List<Entry<String, String>> headers = request.headers().entries();
		if(!headers.isEmpty()) {
			for(Entry<String, String> h : request.headers().entries()) {
				String key = h.getKey();
				String value = h.getValue();
				buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
			}
			buf.append("\r\n");
		}

		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
		Map<String, List<String>> params = queryStringDecoder.parameters();
		if(!params.isEmpty()) {
			for(Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for(String val : vals) {
					buf.append("PARAM: ").append(key).append(" = ").append(val).append("\r\n");
				}
			}
			buf.append("\r\n");
		}

		DecoderResult result = request.getDecoderResult();

		if(result.isSuccess() == false) {
			buf.append(".. WITH DECODER FAILURE: ");
			buf.append(result.cause());
			buf.append("\r\n");
		}
	}
}