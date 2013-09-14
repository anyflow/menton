package net.anyflow.menton.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Set;

import net.anyflow.menton.Configurator;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow Base class for business logic. The class contains common stuffs for generating business logic.
 */
public class RequestHandler {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

	private FullHttpRequest request;
	private FullHttpResponse response;
	private Channel channel;

	public void initialize(Channel channel, FullHttpRequest request, FullHttpResponse response) {
		this.request = request;
		this.response = response;
		this.channel = channel;
	}

	public FullHttpRequest getRequest() {
		return request;
	}

	public FullHttpResponse getResponse() {
		return response;
	}

	public Channel channel() {
		return channel;
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
	 */
	public static Class<? extends RequestHandler> find(String requestedPath, String httpMethod, String requestHandlerPackageRoot) {

		Reflections reflections = new Reflections(requestHandlerPackageRoot);
		String contextRoot = Configurator.instance().getHttpContextRoot();

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
	 */
	public java.lang.reflect.Method findHandler(String requestedPath, String httpMethod) {

		String contextRoot = Configurator.instance().getHttpContextRoot();

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