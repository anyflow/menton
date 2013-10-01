/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author anyflow
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private String requestHandlerPackageRoot;
	private Class<RequestHandler> requestHandlerClass;

	/**
	 * @param clientHandler
	 */
	public ServerChannelInitializer(String requestHandlerPackageRoot) {
		this.requestHandlerPackageRoot = requestHandlerPackageRoot;
	}

	/**
	 * @param requestHandlerClass
	 */
	public ServerChannelInitializer(Class<RequestHandler> requestHandlerClass) {
		this.requestHandlerClass = requestHandlerClass;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		ch.pipeline().addLast("log", new LoggingHandler(LogLevel.DEBUG));
		ch.pipeline().addLast("decoder", new HttpRequestDecoder());
		ch.pipeline().addLast("aggregator", new io.netty.handler.codec.http.HttpObjectAggregator(1048576)); // handle HttpChunks.
		ch.pipeline().addLast("encoder", new HttpResponseEncoder());
		ch.pipeline().addLast("deflater", new HttpContentCompressor()); // automatic content compression.
		ch.pipeline().addLast("handler",
				requestHandlerClass != null ? new HttpServerHandler(requestHandlerClass) : new HttpServerHandler(requestHandlerPackageRoot));
	}
}