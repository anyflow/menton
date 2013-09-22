package net.anyflow.menton.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.anyflow.menton.Configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private static EventLoopGroup bossGroup;
	private static EventLoopGroup workerGroup;

	public HttpServer() {
	}

	public static void start(final ChannelHandler channelHandler) {
		start(channelHandler, Configurator.instance().getHttpPort());
	}

	public static void start(final ChannelHandler channelHandler, int port) {

		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ServerChannelInitializer(channelHandler));
			bootstrap.bind(port).sync();

			logger.info("Menton HTTP server started.");
		}
		catch(InterruptedException e) {
			logger.error("Menton HTTP server failed to start...", e);
			stop();
		}
	}

	public static void stop() {
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