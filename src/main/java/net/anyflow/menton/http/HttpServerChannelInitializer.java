package net.anyflow.menton.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.anyflow.menton.Settings;

class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	final WebSocketFrameHandler webSocketFrameHandler;
	final boolean useSsl;

	public HttpServerChannelInitializer(WebSocketFrameHandler webSocketFrameHandler, boolean useSsl) {
		this.webSocketFrameHandler = webSocketFrameHandler;
		this.useSsl = useSsl;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		// ChannelHandler adding order is 'very' important.
		// HttpServerHandler should be added last after
		// outbound handlers in spite of it is inbound
		// handler.
		// Otherwise, outbound handlers will not be handled.

		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writelogOfNettyLogger"))) {
			ch.pipeline().addLast("log", new LoggingHandler("menton/server", Settings.SELF.logLevel()));
		}

		// if (useSsl) {
		// SSLEngine engine = SslContextProvider.get().createSSLEngine();
		// engine.setUseClientMode(false);
		//
		// //
		// http://www.oracle.com/technetwork/java/javase/documentation/cve-2014-3566-2342133.html
		// //
		// http://www.oracle.com/technetwork/topics/security/poodlecve-2014-3566-2339408.html
		// engine.setEnabledProtocols(new String[] { "SSLv2Hello", "TLSv1",
		// "TLSv1.1", "TLSv1.2" });
		//
		// ch.pipeline().addLast(new SslHandler(engine));
		// }

		if (useSsl) {
			SslContext sslCtx = SslContextBuilder.forServer(Settings.SELF.certChainFile(), Settings.SELF.privateKeyFile())
					.build();

			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}

		ch.pipeline().addLast(new HttpServerCodec());
		ch.pipeline().addLast("aggregato", new HttpObjectAggregator(1048576)); // Handle
																				// HttpChunks.
		ch.pipeline().addLast("deflater", new HttpContentCompressor()); // Automatic
																		// content
																		// compression.
		ch.pipeline().addLast("bizHandler", new HttpServerHandler(webSocketFrameHandler));
	}
}