package net.anyflow.menton.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.io.Files;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import net.anyflow.menton.Configurator;
import net.anyflow.menton.Environment;

/**
 * @author anyflow
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpServerHandler.class);

	private static final String FAILED_TO_FIND_REQUEST_HANDLER = "Failed to find the request handler.";

	private final WebSocketFrameHandler webSocketFrameHandler;
	private WebSocketServerHandshaker webSocketHandshaker = null;

	public HttpServerHandler() {
		webSocketFrameHandler = null;
	}

	public HttpServerHandler(WebSocketFrameHandler webSocketFrameHandler) {
		this.webSocketFrameHandler = webSocketFrameHandler;
	}

	private boolean isWebResourcePath(String path) {

		for(String ext : Configurator.instance().webResourceExtensionToMimes().keySet()) {
			if(path.endsWith("." + ext) == false) {
				continue;
			}
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

		if(msg instanceof FullHttpRequest) {
			FullHttpRequest request = (FullHttpRequest)msg;

			if("WebSocket".equalsIgnoreCase(request.headers().get("Upgrade")) && "Upgrade".equalsIgnoreCase(request.headers().get("Connection"))) {
				if(webSocketFrameHandler == null) { throw new IllegalStateException("webSocketFrameHandler not found"); }

				webSocketHandshaker = (new DefaultWebSocketHandshaker(webSocketFrameHandler.subprotocols())).handshake(ctx, request);
				return;
			}
		}
		else if(msg instanceof WebSocketFrame) {
			if(webSocketHandshaker == null) { throw new IllegalStateException("WebSocketServerHandshaker shouldn't be null"); }
			if(webSocketFrameHandler == null) { throw new IllegalStateException("webSocketFrameHandler not found"); }

			webSocketFrameHandler.handle(webSocketHandshaker, ctx, (WebSocketFrame)msg);
			return;
		}
		else {
			return;
		}

		FullHttpRequest rawRequest = (FullHttpRequest)msg;

		if(HttpHeaders.is100ContinueExpected(rawRequest)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			return;
		}

		HttpResponse response = HttpResponse.createServerDefault(ctx.channel(), rawRequest.headers().get(HttpHeaders.Names.COOKIE));

		String requestPath = new URI(rawRequest.getUri()).getPath();

		if(isWebResourcePath(requestPath)) {
			handleWebResourceRequest(response, requestPath);
		}
		else {
			try {
				processRequest(ctx.channel(), rawRequest, response);
			}
			catch(URISyntaxException e) {
				response.setStatus(HttpResponseStatus.NOT_FOUND);
				logger.info("unexcepted URI : {}", rawRequest.getUri());
			}
			catch(Exception e) {
				response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
				logger.error("Unknown exception was thrown in business logic handler.\r\n" + e.getMessage(), e);
			}
		}

		setDefaultHeaders(rawRequest, response);

		if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		ctx.write(response);
	}

	/**
	 * @param response
	 * @param webResourceRequestPath
	 * @throws IOException
	 */
	private void handleWebResourceRequest(HttpResponse response, String webResourceRequestPath) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(webResourceRequestPath);

		if(is == null) {
			String rootPath = (new File(Configurator.instance().WebResourcePhysicalRootPath(), webResourceRequestPath)).getPath();
			try {
				is = new FileInputStream(rootPath);
			}
			catch(FileNotFoundException e) {
				is = null;
			}
		}

		if(is == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
		}
		else {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			response.content().writeBytes(buffer.toByteArray());

			String ext = Files.getFileExtension(webResourceRequestPath);
			response.headers().set(Names.CONTENT_TYPE, Configurator.instance().webResourceExtensionToMimes().get(ext));

			is.close();
		}
	}

	private void setDefaultHeaders(FullHttpRequest request, HttpResponse response) {

		response.headers().add(Names.SERVER, Environment.PROJECT_ARTIFACT_ID + " " + Environment.PROJECT_VERSION);

		boolean keepAlive = request.headers().get(HttpHeaders.Names.CONNECTION) == HttpHeaders.Values.KEEP_ALIVE;
		if(keepAlive) {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		if(Configurator.instance().getProperty("menton.httpServer.allowCrossDomain", "false").equalsIgnoreCase("true")) {
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, DELETE");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGARUNER");
			response.headers().add(Names.ACCESS_CONTROL_MAX_AGE, "1728000");
		}

		response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
	}

	private void processRequest(Channel channel, FullHttpRequest rawRequest, HttpResponse response)
			throws InstantiationException, IllegalAccessException, IOException, URISyntaxException {

		RequestHandler.MatchedCriterion mc = RequestHandler.findRequestHandler((new URI(rawRequest.getUri())).getPath(),
				rawRequest.getMethod().toString());

		if(mc.requestHandlerClass() == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", rawRequest.getUri());

			response.headers().add(Names.CONTENT_TYPE, "text/html");

			response.setContent(HtmlGenerator.error(FAILED_TO_FIND_REQUEST_HANDLER, response.getStatus()));
		}
		else {
			HttpRequest request = new HttpRequest(channel, rawRequest, mc.pathParameters());

			RequestHandler handler = mc.requestHandlerClass().newInstance();

			handler.initialize(request, response);

			if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpRequest"))) {
				logger.info(request.toString());
			}

			response.setContent(handler.call());
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