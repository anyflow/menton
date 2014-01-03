/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import net.anyflow.menton.exception.DefaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpClient {

	static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private HttpRequest httpRequest;
	
	public HttpClient(String uri, HttpMethod httpMethod) throws URISyntaxException {
		
		httpRequest = new HttpRequest(null, new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri));
	}
	
	public HttpRequest httpRequest() {
		return httpRequest;
	}

	/**
	 * Request with encoding utf-8
	 * 
	 * @param receiver
	 * @return if receiver is not null and the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(final MessageReceiver receiver) throws IllegalArgumentException, UnsupportedEncodingException {
		return request(receiver, "utf-8");
	}

	/**
	 * request.
	 * 
	 * @param receiver
	 * @param queryEncodingCharset
	 *            query encoding charset. if it is null, no encoding will be applied.
	 * @return if receiver is not null the request processed successfully, returns HttpResponse instance, otherwise null;
	 * @throws DefaultException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse request(final MessageReceiver receiver, String queryEncodingCharset) throws IllegalArgumentException,
			UnsupportedEncodingException {

		Thread.currentThread().setName("client/main");

		boolean ssl = false;

		// TODO support HTTPS
		if(!httpRequest().uri().getScheme().equalsIgnoreCase("http") && !httpRequest().uri().getScheme().equalsIgnoreCase("https")) {
			logger.error("Only HTTP(S) is supported.");
			return null;
		}

		final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("client"));

		httpRequest().normalize();
		setHeaders();
	

		if(logger.isDebugEnabled()) {
			logger.debug("[request] URI : {}", httpRequest.getUri());
			logger.debug("[request] CONTENT : {}", httpRequest.content().toString(CharsetUtil.UTF_8));
			logger.debug("[request] HTTPMETHOD : {}", httpRequest.getMethod().toString());

			for(String name : httpRequest.headers().names()) {
				logger.debug("[request] HEADER : " + name + " = " + httpRequest.headers().get(name));
			}
		}

		HttpClientHandler clientHandler = new HttpClientHandler(receiver, httpRequest);

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioSocketChannel.class).handler(new ClientChannelInitializer(clientHandler, ssl));

		try {
			Channel channel = bootstrap.connect(httpRequest().host(), httpRequest().uri().getPort()).sync().channel();
			channel.writeAndFlush(httpRequest);

			if(receiver == null) {
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
		catch(InterruptedException e) {

			group.shutdownGracefully();
			logger.error(e.getMessage(), e);

			return null;
		}
	}

	private void setHeaders() {

		if(httpRequest().getMethod() == HttpMethod.POST && httpRequest().headers().contains(HttpHeaders.Names.CONTENT_TYPE) == false) {
			httpRequest().headers().set(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
		}

		if(httpRequest().headers().contains(HttpHeaders.Names.HOST) == false) {
			httpRequest().headers().set(HttpHeaders.Names.HOST, httpRequest().host() == null ? "localhost" : httpRequest().host());
		}
		if(httpRequest().headers().contains(HttpHeaders.Names.CONNECTION) == false) {
			httpRequest().headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		if(httpRequest().headers().contains(HttpHeaders.Names.ACCEPT_ENCODING) == false) {
			httpRequest().headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP + ", " + HttpHeaders.Values.DEFLATE);
		}
		if(httpRequest().headers().contains(HttpHeaders.Names.ACCEPT_CHARSET) == false) {
			httpRequest().headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
		}
	}
}