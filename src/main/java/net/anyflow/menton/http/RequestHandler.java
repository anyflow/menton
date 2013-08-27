package net.anyflow.menton.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow Base class for business logic. The class contains common
 *         stuffs for generating business logic.
 */
public class RequestHandler {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

	private HttpRequest request;
	private HttpResponse response;
	private MessageEvent messageEvent;
	private Map<String, List<String>> parameters;
	private Map<String, String> headers;

	/**
	 * @return the parameters
	 */
	public Map<String, List<String>> getParameters() {
		return parameters;
	}

	public void initialize(HttpRequest request, HttpResponse response, MessageEvent e) {
		this.request = request;
		this.response = response;
		this.messageEvent = e;

		parseParameters();
		parseHeaders();
	}

	public HttpRequest getRequest() {
		return request;
	}

	public HttpResponse getResponse() {
		return response;
	}
	
	public String getRemoteIpAddress() {
		InetSocketAddress socketAddress = (InetSocketAddress) messageEvent.getRemoteAddress();
		InetAddress inetAddress = socketAddress.getAddress();
		
		return inetAddress.getHostAddress();
	}

	public String getUri() {
		return request.getUri();
	}

	public String getHttpMethod() {
		return request.getMethod().toString();
	}

	public String getProtocolVersion() {
		return request.getProtocolVersion().toString();
	}

	public String getHost() {
		return HttpHeaders.getHost(request, "unknown");
	}

	private void parseParameters() {
		String httpMethod = request.getMethod().toString();
		String queryStringParam = null;

		if(httpMethod.equalsIgnoreCase("GET")) {
			queryStringParam = request.getUri();
		}
		else if(httpMethod.equalsIgnoreCase("POST")) {
			String dummy = "/dummy?";
			queryStringParam = dummy + request.getContent().toString(CharsetUtil.UTF_8);
		}
		else {
			response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED);
			throw new UnsupportedOperationException("only GET/POST http methods are supported.");
		}

		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(queryStringParam);

		parameters = queryStringDecoder.getParameters();
	}

	private void parseHeaders() {
		headers = new HashMap<String, String>();
		List<Entry<String, String>> headerList = this.request.getHeaders();

		Iterator<Entry<String, String>> itr = headerList.iterator();

		while(itr.hasNext()) {
			Entry<String, String> item = itr.next();
			headers.put(item.getKey().toLowerCase(), item.getValue());
		}
	}

	public String getParameter(String key) {
		if(parameters.containsKey(key)) {
			return parameters.get(key).get(0);
		}
		else {
			return "";
		}
	}

	public String getHeader(String key) {
		if(headers.containsKey(key.toLowerCase())) {
			return headers.get(key.toLowerCase());
		}
		else {
			return "";
		}
	}

	public List<String> getArrayParameter(String key) {
		if(parameters.containsKey(key)) {
			return parameters.get(key);
		}
		else {
			return (new ArrayList<String>());
		}
	}

	/**
	 * @return processed content string
	 */
	public String call() {
		throw new UnsupportedOperationException("Derived class's method should be called instead of this.");
	}

	/**
	 * @author anyflow
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Handles {

		/**
		 * @return supported paths
		 */
		String[] paths();

		/**
		 * supported http methods
		 * 
		 * @return http method string
		 */
		String[] httpMethods();
	}

	/**
	 * @param requestedPath
	 * @param httpMethod
	 * @return
	 * @throws DefaultException
	 */
	public static Class<? extends RequestHandler> find(String requestedPath, String httpMethod) throws DefaultException {

		Reflections reflections = new Reflections(Configurator.getRequestHandlerPackageRoot());
		String contextRoot = Configurator.getHttpContextRoot();

		Set<Class<? extends RequestHandler>> requestHandler = reflections.getSubTypesOf(RequestHandler.class);

		for(Class<? extends RequestHandler> item : requestHandler) {

			RequestHandler.Handles bl = item.getAnnotation(RequestHandler.Handles.class);

			if(bl == null) {
				continue;
			}

			for(String method : bl.httpMethods()) {
				if(method.equalsIgnoreCase(httpMethod) == false) {
					continue;
				}

				for(String rawPath : bl.paths()) {

					String path = (rawPath.charAt(0) == '/') ? rawPath : contextRoot + rawPath;

					if(requestedPath.equalsIgnoreCase(path)) { return item; }
				}
			}
		}

		logger.error("Failed to find requestHandler.");
		return null;
	}

	/**
	 * @param requestedPath
	 * @param httpMethod
	 * @return
	 * @throws DefaultException
	 */
	public java.lang.reflect.Method findHandler(String requestedPath, String httpMethod) throws DefaultException {

		String contextRoot = Configurator.getHttpContextRoot();

		Method[] methods = this.getClass().getMethods();

		for(Method item : methods) {

			RequestHandler.Handles bl = item.getAnnotation(RequestHandler.Handles.class);

			if(bl == null) {
				continue;
			}

			for(String method : bl.httpMethods()) {
				if(method.equalsIgnoreCase(httpMethod) == false) {
					continue;
				}

				for(String rawPath : bl.paths()) {

					String path = (rawPath.charAt(0) == '/') ? rawPath : contextRoot + rawPath;

					if(requestedPath.equalsIgnoreCase(path)) { return item; }
				}
			}
		}

		logger.error("Failed to find requestHandler.");
		return null;
	}
}