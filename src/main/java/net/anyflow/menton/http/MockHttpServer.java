package net.anyflow.menton.http;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.anyflow.menton.Configurator;
import net.anyflow.menton.Environment;

public class MockHttpServer {

	private static final Logger logger = LoggerFactory.getLogger(MockHttpServer.class);

	public MockHttpServer(String requestHandlerPakcageRoot) {
		RequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
	}

	public HttpResponse service(HttpRequest httpRequest) {

		HttpResponse response = HttpResponse.createServerDefault(httpRequest.headers().get(HttpHeaders.Names.COOKIE));

		RequestHandler.MatchedCriterion mc = RequestHandler.findRequestHandler(httpRequest.uri().getPath(),
				httpRequest.getMethod().toString());

		if (mc.requestHandlerClass() == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", httpRequest.getUri());

			response.headers().add(Names.CONTENT_TYPE, "text/html");

			response.setContent(HtmlGenerator.error(Literals.FAILED_TO_FIND_REQUEST_HANDLER, response.getStatus()));
		}
		else {
			HttpRequest request;
			try {
				request = new HttpRequest(httpRequest, mc.pathParameters());

				RequestHandler handler;
				try {
					handler = mc.requestHandlerClass().newInstance();
					handler.initialize(request, response);

					if ("true"
							.equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpRequest"))) {
						logger.info(request.toString());
					}

					response.setContent(handler.service());
				}
				catch (InstantiationException | IllegalAccessException | URISyntaxException e) {
					logger.error(e.getMessage(), e);

					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					response.setContent(e.getMessage());
				}
			}
			catch (URISyntaxException e) {
				logger.error(e.getMessage(), e);

				response.setStatus(HttpResponseStatus.BAD_REQUEST);
				response.setContent(e.getMessage());
			}
		}

		setDefaultHeaders(httpRequest, response);

		if ("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		return response;
	}

	private void setDefaultHeaders(FullHttpRequest request, HttpResponse response) {

		response.headers().add(Names.SERVER, Environment.PROJECT_ARTIFACT_ID + " " + Environment.PROJECT_VERSION);

		boolean keepAlive = request.headers().get(HttpHeaders.Names.CONNECTION) == HttpHeaders.Values.KEEP_ALIVE;
		if (keepAlive) {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		if (Configurator.instance().getProperty("menton.httpServer.allowCrossDomain", "false")
				.equalsIgnoreCase("true")) {
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, DELETE");
			response.headers().add(Names.ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGARUNER");
			response.headers().add(Names.ACCESS_CONTROL_MAX_AGE, "1728000");
		}

		response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
	}
}