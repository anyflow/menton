package net.anyflow.menton.http;

import java.util.List;

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

class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(HttpServerChannelInitializer.class);

	final boolean useSsl;
	final WebsocketFrameHandler wsfHandler;

	public HttpServerChannelInitializer(boolean useSsl, WebsocketFrameHandler websocketFrameHandler) {
		this.useSsl = useSsl;
		this.wsfHandler = websocketFrameHandler;
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

		if (wsfHandler != null) {
			ch.pipeline().addLast(WebSocketServerProtocolHandler.class.getName(),
					new WebSocketServerProtocolHandler(wsfHandler.websocketPath(),
							listToCommaSeperatedString(wsfHandler.subprotocols()), wsfHandler.allowExtensions(),
							wsfHandler.maxFrameSize()));

			ch.pipeline().addLast(new WebsocketFrameRouter(wsfHandler));
		}
	}

	private String listToCommaSeperatedString(List<String> target) {
		if (target == null) { return null; }

		StringBuilder sb = new StringBuilder();
		for (String item : target) {
			sb = sb.append(item).append(",");
		}

		String ret = sb.toString();
		return ret.substring(0, ret.length() - 1);
	}
}