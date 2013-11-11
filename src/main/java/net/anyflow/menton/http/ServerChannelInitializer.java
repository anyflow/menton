/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
import net.anyflow.menton.Configurator;

/**
 * @author anyflow
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private String requestHandlerPackageRoot;
	private Class<? extends RequestHandler> requestHandlerClass;

	/**
	 * @param clientHandler
	 */
	public ServerChannelInitializer(String requestHandlerPackageRoot) {
		this.requestHandlerPackageRoot = requestHandlerPackageRoot;
	}

	/**
	 * @param requestHandlerClass
	 */
	public ServerChannelInitializer(Class<? extends RequestHandler> requestHandlerClass) {
		this.requestHandlerClass = requestHandlerClass;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		// ChannelHandler adding order is 'very' important.
		// HttpServerHandler should be added last after outbound handlers in spite of it is inbound handler.
		// Otherwise, outbound handlers will not be handled.

		if("true".equalsIgnoreCase(Configurator.instance().getProperty("write_netty_log"))) {
			ch.pipeline().addLast("log", new LoggingHandler("menton/server", Configurator.instance().getLogLevel()));
		}

		ch.pipeline().addLast("decoder", new HttpRequestDecoder());
		ch.pipeline().addLast("aggregator", new io.netty.handler.codec.http.HttpObjectAggregator(1048576)); // Handle HttpChunks.
		ch.pipeline().addLast("encoder", new HttpResponseEncoder());
		ch.pipeline().addLast("deflater", new HttpContentCompressor()); // Automatic content compression.
		ch.pipeline().addLast("bizHandler",
				requestHandlerClass != null ? new HttpServerHandler(requestHandlerClass) : new HttpServerHandler(requestHandlerPackageRoot));
	}
}