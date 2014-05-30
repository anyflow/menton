/**
 * 
 */
package net.anyflow.menton.http;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.io.Files;

/**
 * @author anyflow
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpServerHandler.class);

	private static final String FAILED_TO_FIND_REQUEST_HANDLER = "Failed to find the request handler.";

	private static final Map<String, String> FILE_REQUEST_EXTENSIONS;

	static {
		FILE_REQUEST_EXTENSIONS = new HashMap<String, String>();

		try {
			JSONObject obj = new JSONObject(Configurator.instance().getProperty("menton.httpServer.MIME"));
			@SuppressWarnings("unchecked")
			Iterator<String> keys = obj.keys();

			while(keys.hasNext()) {
				String key = keys.next();
				FILE_REQUEST_EXTENSIONS.put(key, obj.get(key).toString());
			}
		}
		catch(JSONException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private final WebSocketFrameHandler webSocketFrameHandler;
	private WebSocketServerHandshaker webSocketHandshaker = null;

	public HttpServerHandler() {
		webSocketFrameHandler = null;
	}

	public HttpServerHandler(WebSocketFrameHandler webSocketFrameHandler) {
		this.webSocketFrameHandler = webSocketFrameHandler;
	}

	private String getWebResourceRequestPath(HttpRequest request) {

		String path;
		try {
			path = new URI(request.getUri()).getPath();
		}
		catch(URISyntaxException e) {
			return null;
		}

		for(String ext : FILE_REQUEST_EXTENSIONS.keySet()) {
			if(path.endsWith("." + ext) == false) {
				continue;
			}
			return path;
		}

		return null;
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
				webSocketHandshaker = (new DefaultWebSocketHandshaker()).handshake(ctx, request);
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

		HttpRequest request = new HttpRequest(ctx.channel(), (FullHttpRequest)msg);

		if(HttpHeaders.is100ContinueExpected(request)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			return;
		}

		if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpRequest"))) {
			logger.info(request.toString());
		}

		HttpResponse response = HttpResponse.createServerDefault(ctx.channel(), request.headers().get(HttpHeaders.Names.COOKIE));

		String webResourceRequestPath = getWebResourceRequestPath(request);

		if(webResourceRequestPath != null) {
			handleWebResourceRequest(response, webResourceRequestPath);
		}
		else {
			try {
				String path = (new URI(request.getUri())).getPath();

				String content = handleClassTypeHandler(request, response, path);

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
		}

		setDefaultHeaders(request, response);

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
			response.headers().set(Names.CONTENT_TYPE, FILE_REQUEST_EXTENSIONS.get(ext));

			is.close();
		}
	}

	private void setDefaultHeaders(HttpRequest request, HttpResponse response) {

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

	private String handleClassTypeHandler(HttpRequest request, HttpResponse response, String requestedPath) throws InstantiationException,
			IllegalAccessException, IOException {

		Class<? extends RequestHandler> handlerClass = RequestHandler.findClass(requestedPath, request.getMethod().toString());

		if(handlerClass == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", request.getUri().toString());

			response.headers().add(Names.CONTENT_TYPE, "text/html");

			return HtmlGenerator.error(FAILED_TO_FIND_REQUEST_HANDLER, response.getStatus());
		}

		RequestHandler handler = handlerClass.newInstance();

		handler.initialize(request, response);

		return handler.call();
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

	// private String handleMethodTypeHandler(HttpRequest request, HttpResponse response, String requestedPath) throws IllegalAccessException,
	// InvocationTargetException, IllegalArgumentException, SecurityException, InstantiationException, IOException {
	//
	// RequestHandler requestHandler = (RequestHandler)requestHandlerClass.getConstructors()[0].newInstance();
	//
	// requestHandler.initialize(request, response);
	// Method handler = requestHandler.findMethod(requestedPath, request.getMethod().toString());
	//
	// if(handler == null) {
	// response.setStatus(HttpResponseStatus.NOT_FOUND);
	// logger.info("unexcepted URI : {}", request.getUri().toString());
	//
	// response.headers().add(Names.CONTENT_TYPE, "text/html");
	//
	// return HtmlGenerator.error(FAILED_TO_FIND_REQUEST_HANDLER, response.getStatus());
	// }
	//
	// return handler.invoke(requestHandler, (Object[])null).toString();
	// }

}