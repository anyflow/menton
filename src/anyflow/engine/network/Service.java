package anyflow.engine.network;

import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 * Base class for business logic. The class contains common stuffs for generating business logic.
 */
public class Service {

	private static final Logger logger = LoggerFactory.getLogger(Service.class);
	
	private HttpRequest request;
	private HttpResponse response;
	
	public void initialize(HttpRequest request, HttpResponse response) {
		this.request = request;
		this.response = response;
	}

	public HttpRequest getRequest() {
		return request;
	}
	
	public HttpResponse getResponse() {
		return response;
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
	
	public String getParameter(String key) {
		String httpMethod = request.getMethod().toString();
		String queryStringParam = null;
		
        if(httpMethod.equalsIgnoreCase("GET")) {
        	queryStringParam = request.getUri();
        }
        else if(httpMethod.equalsIgnoreCase("POST")) {
        	String dummy = "/dummy?";
        	queryStringParam =  dummy + request.getContent().toString(CharsetUtil.UTF_8);
        }
        else {
        	response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED);
        	throw new UnsupportedOperationException("only GET/POST http methods are supported.");
        }
        
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(queryStringParam);
        
        final Map<String, List<String>> params = queryStringDecoder.getParameters();
        if(params.containsKey(key) == false) { 
        	return null;  
        }
        
        return params.get(key).get(0);
	}
}