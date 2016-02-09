package net.anyflow.menton.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.anyflow.menton.Settings;

class WebServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebServerChannelInitializer.class);

	final boolean useSsl;
	final WebsocketFrameHandler websocketFrameHandler;

	public WebServerChannelInitializer(boolean useSsl, WebsocketFrameHandler websocketFrameHandler) {
		this.useSsl = useSsl;
		this.websocketFrameHandler = websocketFrameHandler;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writelogOfNettyLogger"))) {
			ch.pipeline().addLast("log", new LoggingHandler("menton/server", Settings.SELF.logLevel()));
		}

		if (useSsl) {
			SslContext sslCtx = SslContextBuilder
					.forServer(Settings.SELF.certChainFile(), Settings.SELF.privateKeyFile()).build();

			logger.debug("SSL Provider : {}", SslContext.defaultServerProvider());

			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}

		ch.pipeline().addLast(new HttpServerCodec());
		ch.pipeline().addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(1048576));
		ch.pipeline().addLast(HttpContentCompressor.class.getName(), new HttpContentCompressor());
		ch.pipeline().addLast(HttpRequestRouter.class.getName(), new HttpRequestRouter());

		if (websocketFrameHandler != null) {
			ch.pipeline().addLast(WebSocketServerProtocolHandler.class.getName(),
					new WebSocketServerProtocolHandler(websocketFrameHandler.websocketPath(),
							websocketFrameHandler.subprotocols(), websocketFrameHandler.allowExtensions(),
							websocketFrameHandler.maxFrameSize()));

			ch.pipeline().addLast(websocketFrameHandler);
		}
	}
}