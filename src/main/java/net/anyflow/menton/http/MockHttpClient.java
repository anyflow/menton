package net.anyflow.menton.http;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class MockHttpClient implements IHttpClient {

	static final Logger logger = LoggerFactory.getLogger(MockHttpClient.class);

	private final HttpRequest httpRequest;
	private final MockHttpServer mockServer;

	public MockHttpClient(MockHttpServer mockServer, String uri) throws URISyntaxException {
		this.httpRequest = new HttpRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));
		this.mockServer = mockServer;

		if (httpRequest().uri().getScheme().equalsIgnoreCase("http") == false) {
			String message = "HTTP is supported only.";
			logger.error(message);
			throw new UnsupportedOperationException(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#httpRequest()
	 */
	@Override
	public HttpRequest httpRequest() {
		return httpRequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#get()
	 */
	@Override
	public HttpResponse get() {
		return get(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#get(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse get(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.GET);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#post()
	 */
	@Override
	public HttpResponse post() {
		return post(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#post(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse post(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.POST);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#put()
	 */
	@Override
	public HttpResponse put() {
		return put(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#put(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse put(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.PUT);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#delete()
	 */
	@Override
	public HttpResponse delete() {
		return delete(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#delete(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse delete(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.DELETE);

		return request(receiver);
	}

	@Override
	public <T> IHttpClient setOption(ChannelOption<T> option, T value) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * request.
	 * 
	 * @param receiver
	 * @return if receiver is not null the request processed successfully,
	 *         returns HttpResponse instance, otherwise null.
	 */
	private HttpResponse request(final MessageReceiver receiver) {

		httpRequest().normalize();
		setDefaultHeaders();

		if (logger.isDebugEnabled()) {
			logger.debug(httpRequest().toString());
		}

		HttpResponse response = mockServer.service(httpRequest());
		if (receiver != null) {
			receiver.messageReceived(httpRequest(), response);
			return null;
		}
		else {
			return response;
		}
	}

	private void setDefaultHeaders() {
		if (httpRequest().headers().contains(HttpHeaders.Names.HOST) == false) {
			httpRequest().headers().set(HttpHeaders.Names.HOST, httpRequest().uri().getHost());
		}
		if (httpRequest().headers().contains(HttpHeaders.Names.CONNECTION) == false) {
			httpRequest().headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		if (httpRequest().headers().contains(HttpHeaders.Names.ACCEPT_ENCODING) == false) {
			httpRequest().headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
					HttpHeaders.Values.GZIP + ", " + HttpHeaders.Values.DEFLATE);
		}
		if (httpRequest().headers().contains(HttpHeaders.Names.ACCEPT_CHARSET) == false) {
			httpRequest().headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
		}
		if (httpRequest().headers().contains(HttpHeaders.Names.CONTENT_TYPE) == false) {
			httpRequest().headers().set(HttpHeaders.Names.CONTENT_TYPE,
					HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
		}
	}
}
