package net.anyflow.menton.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private static ChannelGroup channelGroup;
	private static EventLoopGroup bossGroup;
	private static EventLoopGroup workerGroup;

	public HttpServer() {
	}

	public static void start(final ChannelHandler channelHandler) throws DefaultException {
		start(channelHandler, Configurator.getHttpPort());
	}

	public static void start(final ChannelHandler channelHandler, int port) {

		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {

					ch.pipeline().addLast("decoder", new HttpRequestDecoder());

					// Uncomment the following line if you don't want to handle HttpChunks.
					ch.pipeline().addLast("aggregator", new HttpRequestDecoder());

					ch.pipeline().addLast("encoder", new HttpResponseEncoder());

					// Remove the following line if you don't want automatic content compression.
					ch.pipeline().addLast("deflater", new HttpContentCompressor());

					ch.pipeline().addLast("handler", new HttpServerHandler());
				}

			});

			Channel ch = bootstrap.bind(port).sync().channel();
			channelGroup.add(ch);
			// ch.closeFuture().sync();

			logger.info("Menton HTTP server started.");
		}
		catch(InterruptedException e) {
			logger.error("Menton HTTP server failed to start...", e);
			stop();
		}
	}

	public static void stop() {
		if(channelGroup != null) {
			channelGroup.close().awaitUninterruptibly();
			logger.debug("Channel group closed.");
		}

		if(bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Boss event loop group shutdowned.");
		}

		if(workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Worker event loop group shutdowned.");
		}

		logger.debug("Menton HTTP server stopped.");
	}
}