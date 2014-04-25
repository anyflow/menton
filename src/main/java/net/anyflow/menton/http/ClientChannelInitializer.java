/**
 * 
 */
package net.anyflow.menton.http;

import net.anyflow.menton.Configurator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author anyflow
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final HttpClientHandler clientHandler;
	private final boolean ssl;

	/**
	 * @param clientHandler
	 */
	public ClientChannelInitializer(HttpClientHandler clientHandler, boolean ssl) {
		this.clientHandler = clientHandler;
		this.ssl = ssl;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		ChannelPipeline p = ch.pipeline();
		
		if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writelogOfNettyLogger"))) {
			p.addLast("log", new LoggingHandler("menton/client", Configurator.instance().logLevel()));
		}
		
		if(ssl) {
			// TODO
			// SSL related..
			// p.addLast("ssl", new SslHandler(engine));
		}

		p.addLast("codec", new HttpClientCodec());
		p.addLast("inflater", new HttpContentDecompressor());
		p.addLast("chunkAggregator", new HttpObjectAggregator(1048576));
		p.addLast("handler", clientHandler);
	}
}