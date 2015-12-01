/**
 * 
 */
package net.anyflow.menton.http;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author anyflow
 */
public class HttpClient implements IHttpClient {

	static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	final Bootstrap bootstrap;
	private final HttpRequest httpRequest;

	public HttpClient(String uri) throws URISyntaxException, UnsupportedOperationException {

		bootstrap = new Bootstrap();

		httpRequest = new HttpRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#setOption(io.netty.channel.ChannelOption,
	 * T)
	 */
	@Override
	public <T> IHttpClient setOption(ChannelOption<T> option, T value) {
		bootstrap.option(option, value);

		return this;
	}

	/**
	 * request.
	 * 
	 * @param receiver
	 * @return if receiver is not null the request processed successfully,
	 *         returns HttpResponse instance, otherwise null.
	 */
	private HttpResponse request(final MessageReceiver receiver) {

		boolean ssl = false;

		httpRequest().normalize();
		setDefaultHeaders();

		if (logger.isDebugEnabled()) {
			logger.debug(httpRequest().toString());
		}

		HttpClientHandler clientHandler = new HttpClientHandler(receiver, httpRequest);

		final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("client"));
		bootstrap.group(group).channel(NioSocketChannel.class)
				.handler(new ClientChannelInitializer(clientHandler, ssl));

		try {
			Channel channel = bootstrap.connect(httpRequest().uri().getHost(), httpRequest().uri().getPort()).sync()
					.channel();
			channel.writeAndFlush(httpRequest);

			if (receiver == null) {
				channel.closeFuture().sync();
				group.shutdownGracefully();

				return clientHandler.httpResponse();
			}
			else {
				channel.closeFuture().addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						group.shutdownGracefully();
					}
				});

				return null;
			}
		}
		catch (Exception e) {
			group.shutdownGracefully();
			logger.error(e.getMessage(), e);

			return null;
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