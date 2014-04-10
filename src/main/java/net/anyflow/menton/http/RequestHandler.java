package net.anyflow.menton.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.anyflow.menton.Configurator;

import org.reflections.Reflections;

/**
 * @author anyflow Base class for business logic. The class contains common stuffs for generating business logic.
 */
public class RequestHandler {

	private static Map<String, Class<? extends RequestHandler>> handlerClassMap;
	private static Map<String, Method> handlerMethodMap;
	private static Set<Class<? extends RequestHandler>> requestHandler;

	private HttpRequest request;
	private HttpResponse response;

	static {
		handlerClassMap = new HashMap<String, Class<? extends RequestHandler>>();
		handlerMethodMap = new HashMap<String, Method>();
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
	public String call() {
		throw new UnsupportedOperationException("Derived class's method should be called instead of this.");
	}

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

	/**
	 * @param requestedPath
	 * @param httpMethod
	 * @param requestHandlerPackageRoot
	 * @return
	 */
	public static Class<? extends RequestHandler> findClass(String requestedPath, String httpMethod) {

		String findKey = requestedPath + httpMethod;

		if(handlerClassMap.containsKey(findKey)) { return handlerClassMap.get(findKey); }

		if(requestHandler == null) {
			requestHandler = (new Reflections(Configurator.instance().requestHandlerPackageRoot())).getSubTypesOf(RequestHandler.class);
		}

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

					String path = (rawPath.charAt(0) == '/') ? rawPath : Configurator.instance().httpContextRoot() + rawPath;

					if(requestedPath.equalsIgnoreCase(path)) {
						handlerClassMap.put(findKey, item);
						return item;
					}
				}
			}
		}

		handlerClassMap.put(findKey, null);
		return null;
	}
}