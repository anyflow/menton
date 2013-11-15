package net.anyflow.menton.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
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

	public static void start(String requestHandlerPackageRoot) {
		start(requestHandlerPackageRoot, null, Configurator.instance().getHttpPort());
	}

	public static void start(Class<? extends RequestHandler> requestHandlerClass) {
		start(null, requestHandlerClass, Configurator.instance().getHttpPort());
	}

	public static void start(String requestHandlerPackageRoot, int port) {
		start(requestHandlerPackageRoot, null, port);
	}

	public static void start(Class<? extends RequestHandler> requestHandlerClass, int port) {
		start(null, requestHandlerClass, port);
	}

	private static void start(String requestHandlerPackageRoot, Class<? extends RequestHandler> requestHandlerClass, int port) {
		Thread.currentThread().setName("server/main");

		bossGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.bossThreadCount", 0), new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.workerThreadCount", 0), new DefaultThreadFactory("server/worker"));

		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			ServerChannelInitializer serverChannelInitializer = requestHandlerClass != null ? new ServerChannelInitializer(requestHandlerClass)
					: new ServerChannelInitializer(requestHandlerPackageRoot);

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(serverChannelInitializer);
			bootstrap.bind(port).sync();

			logger.info("Menton HTTP server started.");
		}
		catch(InterruptedException e) {
			logger.error("Menton HTTP server failed to start...", e);
			shutdown();
		}
	}

	public static void shutdown() {
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