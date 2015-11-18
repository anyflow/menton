package net.anyflow.menton.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import com.google.common.collect.Maps;
import com.google.inject.internal.Lists;

import net.anyflow.menton.Configurator;

/**
 * Base class for request handler. The class contains common stuffs for
 * generating business logic.
 * 
 * @author anyflow
 */
public abstract class RequestHandler {

	private static final Map<String, Class<? extends RequestHandler>> handlerClassMap = Maps.newHashMap();
	private static Set<Class<? extends RequestHandler>> requestHandlerClasses;
	private static String requestHandlerPakcageRoot;

	private HttpRequest request;
	private HttpResponse response;

	private RequestHandler.Handles annotation;

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
	 * @return processed response body string
	 */
	public abstract String service();

	public void initialize(HttpRequest request, HttpResponse response) throws URISyntaxException {
		this.request = request;
		this.response = response;
	}

	public HttpRequest httpRequest() {
		return request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	public String[] handlingHttpMethods() {
		if (annotation == null) {
			annotation = this.getClass().getAnnotation(RequestHandler.Handles.class);
		}

		if (annotation == null) { return null; }

		return annotation.httpMethods();
	}

	public String[] handlingPaths() {
		if (annotation == null) {
			annotation = this.getClass().getAnnotation(RequestHandler.Handles.class);
		}

		if (annotation == null) { return null; }

		return annotation.paths();
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
	public static MatchedCriterion findRequestHandler(String requestedPath, String httpMethod) {

		for (String criterion : handlerClassMap.keySet()) {
			MatchedCriterion mc = match(requestedPath, httpMethod, criterion);

			if (mc.result == true) {
				mc.requestHandlerClass = handlerClassMap.get(criterion);
				return mc;
			}
		}

		if (requestHandlerClasses == null) {
			requestHandlerClasses = (new Reflections(requestHandlerPakcageRoot)).getSubTypesOf(RequestHandler.class);
		}

		for (Class<? extends RequestHandler> item : requestHandlerClasses) {

			RequestHandler.Handles annotation = item.getAnnotation(RequestHandler.Handles.class);

			if (annotation == null) {
				continue;
			}

			for (String method : annotation.httpMethods()) {
				if (method.equalsIgnoreCase(httpMethod) == false) {
					continue;
				}

				for (String rawPath : annotation.paths()) {

					String path = (rawPath.charAt(0) == '/') ? rawPath
							: Configurator.instance().httpContextRoot() + rawPath;
					String criterion = path + "/" + method;

					MatchedCriterion mc = match(requestedPath, method, criterion);
					if (mc.result == false) {
						continue;
					}

					handlerClassMap.put(criterion, item);
					mc.requestHandlerClass = item;

					return mc;
				}
			}
		}

		return new MatchedCriterion();
	}

	public static class MatchedCriterion {

		private boolean result;
		private Class<? extends RequestHandler> requestHandlerClass;
		private String criterionPath;
		private String criterionHttpMethod;
		private final Map<String, String> pathParameters = Maps.newHashMap();

		public Class<? extends RequestHandler> requestHandlerClass() {
			return requestHandlerClass;
		}

		public String criterionPath() {
			return criterionPath;
		}

		public String criterionHttpMethod() {
			return criterionHttpMethod;
		}

		public Map<String, String> pathParameters() {
			return pathParameters;
		}
	}

	private static MatchedCriterion match(String requestedPath, String httpMethod, String criterion) {

		MatchedCriterion ret = new MatchedCriterion();

		String[] criterionTokens = criterion.split("/");

		List<String> testTokens = Lists.newArrayList(requestedPath.split("/"));
		testTokens.add(httpMethod);

		if (criterionTokens.length != testTokens.size()) { return ret; }

		for (int i = 1; i < criterionTokens.length; ++i) { // should start with
															// #1 due to item[0]
															// is whitespace.
			if (criterionTokens[i].startsWith("{") && criterionTokens[i].endsWith("}")) {
				ret.pathParameters.put(criterionTokens[i].substring(1, criterionTokens[i].length() - 1),
						testTokens.get(i));
			}
			else if (criterionTokens[i].equalsIgnoreCase(testTokens.get(i)) == false) { return ret; }
		}

		ret.result = true;
		ret.criterionHttpMethod = httpMethod;
		ret.criterionPath = criterion.replace("/" + httpMethod, "");

		return ret;
	}
}