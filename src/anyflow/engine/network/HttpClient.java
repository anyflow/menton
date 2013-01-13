/**
 * 
 */
package anyflow.engine.network;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 *
 */
public class HttpClient {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private final URI uri;
	private HttpMethod httpMethod;
	private Map<String, String> cookies;
	
	public HttpClient(URI uri) {
		this.uri = uri;
		
		httpMethod = HttpMethod.GET;
		cookies = new HashMap<String, String>();
	}
	

	/**
	 * request.
	 * @param isSynchronousMode
	 * @param receiver
	 * @return if isSynchronousMode is true and the request processed successfully, returns HttpResponse instance, otherwise null; 
	 */
	public HttpResponse request(boolean isSynchronousMode, final MessageReceiver receiver) {
		
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "localhost" : uri.getHost();

		int port = uri.getPort();
		if(port == -1) {
			if(scheme.equalsIgnoreCase("http")) {
				port = 80;
			} else if(scheme.equalsIgnoreCase("https")) {
				port = 443;
			}
		}
		
		if(!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
			logger.error("Only HTTP(S) is supported.");
			return null;
		}
		
		final ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool()
																						, Executors.newCachedThreadPool()));
		
//		final boolean isSsl = scheme.equalsIgnoreCase("https");

		final HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri.getRawPath());
		final HttpClientHandler clientHandler = new HttpClientHandler(receiver, request);
		
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = org.jboss.netty.channel.Channels.pipeline();
				
				//TODO implement SSL relevant Stuffs..
//				if(isSsl) {
//					SSLEngine engine = SecureChatSslContextFactory.getClientContext().createSSLEngine();
//					engine.setUseClientMode(true);
//					
//					pipeline.addLast("ssl", new SslHandler(engine));
//				}
				
				pipeline.addLast("codec", new HttpClientCodec());
				pipeline.addLast("inflateer", new HttpContentDecompressor());
				pipeline.addLast("handler", clientHandler);
				return pipeline;
			}
		});
		
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
		
		Channel channel = future.awaitUninterruptibly().getChannel();
		if(!future.isSuccess()) {
			logger.error("connection failed.", future.getCause());
			bootstrap.releaseExternalResources();
			return null;
		}
		
		channel = future.awaitUninterruptibly().getChannel();
		channel.getCloseFuture().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception {
				new Thread(new Runnable() {

					@Override
					public void run() {
						bootstrap.releaseExternalResources();
					}
				}).start();
			}
		});
		
		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		
		CookieEncoder httpCookieEncoder = new CookieEncoder(false);
		
		for(Entry<String, String> item : cookies.entrySet()) {
			httpCookieEncoder.addCookie(item.getKey(), item.getValue());
		}
		request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
		
		channel.write(request);
		
		if(isSynchronousMode == false) {
			return null;
		}
		
		try {
			channel.getCloseFuture().await();
			return clientHandler.getResponse();
		} 
		catch (InterruptedException e) {
			logger.error("waiting interrupted.", e);
			return null;
		}
	}
	
	/**
	 * @param httpMethod
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}
	
	/**
	 * @param key
	 * @param value
	 */
	public void setCookie(String key, String value) {
		cookies.put(key, value);
	}
	
	/**
	 * @return
	 */
	public Map<String, String> getCookies() {
		return cookies;
	}

	public void addParameter(String key, String value) {
		//TODO implement setParameters
		
	}
	
	/**
	 * @author anyflow
	 *
	 */
	public class HttpClientHandler extends SimpleChannelUpstreamHandler {
		
		private boolean readingChunks;
		private MessageReceiver receiver;
		private HttpRequest request;
		private HttpResponse response;
		
		public HttpClientHandler(MessageReceiver receiver, HttpRequest request) {
			this.receiver = receiver;
			this.request = request;
		}
		
		public HttpResponse getResponse() {
			return response;
		}
		
		//TODO chunk mode handling.. especially, receiver.messageReceived.
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if(!readingChunks) {
				response = (HttpResponse) e.getMessage();
				
				logger.info("STATUS : " + response.getStatus());
				logger.info("VERSION : " + response.getProtocolVersion());
				
				if(!response.getHeaderNames().isEmpty()) {
					for(String name : response.getHeaderNames()) {
						for(String value : response.getHeaders(name)) {
							logger.info("HEADER : " + name + " = " + value);
						}
					}
				}
				
				if(response.isChunked()) {
					readingChunks = true;
					logger.info("CHUNKED CONTENT {");
				}
				else {
					ChannelBuffer content = response.getContent();
					if(content.readable()) {
						logger.info("CONTENT {");
						logger.info(content.toString(CharsetUtil.UTF_8));
						logger.info("} END OF CONTENT");
					}
					
					if(receiver == null) { return; }
					
					receiver.messageReceived(request, response);
				}
					
			}
			else {
				HttpChunk chunk = (HttpChunk)e.getMessage();
				if(chunk.isLast()) {
					readingChunks = false;
					logger.info("} END OF CHUNKED CONTENT");
				}
				else {
					logger.info(chunk.getContent().toString(CharsetUtil.UTF_8));
				}
			}
		}
	}
}