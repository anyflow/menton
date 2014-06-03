package net.anyflow.menton.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import net.anyflow.menton.Configurator;

import org.reflections.Reflections;

import com.google.common.collect.Maps;

/**
 * Base class for request handler. The class contains common stuffs for generating business logic.
 * 
 * @author anyflow
 */
public abstract class RequestHandler {

	private static final Map<String, Class<? extends RequestHandler>> handlerClassMap = Maps.newHashMap();
	private static final Map<String, Method> handlerMethodMap = Maps.newHashMap();
	private static Set<Class<? extends RequestHandler>> requestHandlerClasses;
	private static String requestHandlerPakcageRoot;

	private HttpRequest request;
	private HttpResponse response;

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

	public void initialize(HttpRequest request, HttpResponse response) {
		this.request = request;
		this.response = response;
	}

	public HttpRequest httpRequest() {
		return request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	/**
	 * @return processed content string
	 */
	public abstract String call();

	/**
	 * @param requestedPath
	 * @param httpMethod
	 * @return
	 */
	public java.lang.reflect.Method findMethod(String requestedPath, String httpMethod) {

		String findKey = requestedPath + httpMethod;

		if(handlerMethodMap.containsKey(findKey)) { return handlerMethodMap.get(findKey); }

		String contextRoot = Configurator.instance().httpContextRoot();

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

					if(requestedPath.equalsIgnoreCase(path)) {
						handlerMethodMap.put(findKey, item);
						return item;
					}
				}
			}
		}

		handlerMethodMap.put(findKey, null);
		return null;
	}

	public static void setRequestHandlerPakcageRoot(String requestHandlerPakcageRoot) {
		RequestHandler.requestHandlerPakcageRoot = requestHandlerPakcageRoot;
	}

	/**
	 * @param requestedPath
	 * @param httpMethod
	 * @param requestHandlerPackageRoot
	 * @return
	 */
	public static Class<? extends RequestHandler> findClass(String requestedPath, String httpMethod) {

		String key = requestedPath + httpMethod;

		if(handlerClassMap.containsKey(key)) { return handlerClassMap.get(key); }

		if(requestHandlerClasses == null) {
			requestHandlerClasses = (new Reflections(requestHandlerPakcageRoot)).getSubTypesOf(RequestHandler.class);
		}

		for(Class<? extends RequestHandler> item : requestHandlerClasses) {

			RequestHandler.Handles bl = item.getAnnotation(RequestHandler.Handles.class);

			if(bl == null) {
				continue;
			}

			for(String method : bl.httpMethods()) {
				if(method.equalsIgnoreCase(httpMethod) == false) {
					continue;
				}

				for(String rawPath : bl.paths()) {

					String path = (rawPath.charAt(0) == '/') ? rawPath : Configurator.instance().httpContextRoot() + rawPath;

					if(requestedPath.equalsIgnoreCase(path)) {
						handlerClassMap.put(key, item);
						return item;
					}
				}
			}
		}

		handlerClassMap.put(key, null);
		return null;
	}
}