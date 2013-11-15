/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.logging.LoggingHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

/**
 * @author anyflow
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpServerHandler.class);

	private HttpRequest request;
	private String requestHandlerPackageRoot;
	private Class<? extends RequestHandler> requestHandlerClass;

	public HttpServerHandler(String requestHandlerPackageRoot) {
		this.requestHandlerPackageRoot = requestHandlerPackageRoot;
	}

	/**
	 * @param requestHandlerClass
	 */
	public HttpServerHandler(Class<? extends RequestHandler> requestHandlerClass) {
		this.requestHandlerClass = requestHandlerClass;
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

		request = new HttpRequest(ctx.channel(), msg);

		if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpRequest"))) {
			logger.info(request.toString());
		}

		HttpResponse response = HttpResponse.createServerDefault(ctx.channel(), request.headers().get(HttpHeaders.Names.COOKIE));

		try {
			String path = (new URI(request.getUri())).getPath();

			String content = requestHandlerClass != null ? handleMethodTypeHandler(request, response, path) : handleClassTypeHandler(request,
					response, path);

			response.setContent(content);
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
			logger.error("Failed to access business logic handler.", e);
		}
		catch(InvocationTargetException e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Unknown exception was thrown in business logic handler. Look into exception parents.", e);
		}
		catch(IllegalArgumentException e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Failed to access business logic handler.", e);
		}
		catch(SecurityException e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Failed to access business logic handler.", e);
		}
		catch(Exception e) {
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logger.error("Unknown exception was thrown in business logic handler. Look into exception parents.", e);
		}

		boolean keepAlive = request.headers().get(HttpHeaders.Names.CONNECTION) == HttpHeaders.Values.KEEP_ALIVE;
		if(keepAlive) {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		if(Configurator.instance().getProperty("menton.httpServer.allowCrossDomain", "false").equalsIgnoreCase("true")) {
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGARUNER");
			response.headers().add(Names.ACCESS_CONTROL_MAX_AGE, "1728000");
		}
		
		response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());

		if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}
		
		ctx.write(response);
	}

	private String handleClassTypeHandler(HttpRequest request, HttpResponse response, String requestedPath) throws DefaultException,
			InstantiationException, IllegalAccessException {

		Class<? extends RequestHandler> handlerClass = RequestHandler.find(requestedPath, request.getMethod().toString(),
				this.requestHandlerPackageRoot);

		if(handlerClass == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());

			return "Failed to find the request handler.";
		}
		else {
			RequestHandler handler = handlerClass.newInstance();

			handler.initialize(request, response);

			return handler.call();
		}
	}

	private String handleMethodTypeHandler(HttpRequest request, HttpResponse response, String requestedPath) throws DefaultException,
			IllegalAccessException, InvocationTargetException, IllegalArgumentException, SecurityException, InstantiationException {

		RequestHandler requestHandler = (RequestHandler)requestHandlerClass.getConstructors()[0].newInstance();

		requestHandler.initialize(request, response);
		Method handler = requestHandler.find(requestedPath, request.getMethod().toString());

		if(handler == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());

			return "Failed to find the request handler.";
		}
		else {
			return handler.invoke(requestHandler, (Object[])null).toString();
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		ctx.close();
	}
}