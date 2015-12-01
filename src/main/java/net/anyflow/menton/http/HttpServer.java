package net.anyflow.menton.http;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.menton.Configurator;
import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

/**
 * @author anyflow
 */
public class HttpServer implements TaskCompletionInformer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private final List<TaskCompletionListener> taskCompletionListeners;

	public HttpServer() {
		taskCompletionListeners = Lists.newArrayList();

		bossGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.bossThreadCount", 0),
				new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.workerThreadCount", 0),
				new DefaultThreadFactory("server/worker"));
	}

	public EventLoopGroup bossGroup() {
		return bossGroup;
	}

	public EventLoopGroup workerGroup() {
		return workerGroup;
	}

	/**
	 * @param requestHandlerPakcageRoot
	 *            root package prefix of request handlers.
	 * @return the HTTP channel
	 */
	public void start(String requestHandlerPakcageRoot) {
		start(requestHandlerPakcageRoot, null);
	}

	/**
	 * @param requestHandlerPakcageRoot
	 *            root package prefix of request handlers.
	 * @param webSocketFrameHandler
	 *            websocket handler
	 * @return the HTTP channel
	 */
	public void start(String requestHandlerPakcageRoot, final WebSocketFrameHandler webSocketFrameHandler) {
		RequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
		try {
			if (Configurator.instance().httpPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new HttpServerChannelInitializer(webSocketFrameHandler, false));

				bootstrap.bind(Configurator.instance().httpPort()).sync();
			}

			if (Configurator.instance().httpsPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new HttpServerChannelInitializer(webSocketFrameHandler, true));

				bootstrap.bind(Configurator.instance().httpPort()).sync();
			}

			logger.info("Menton HTTP server started.");
		}
		catch (Exception e) {
			logger.error("Menton HTTP server failed to start...", e);
			shutdown();
		}
	}

	public void shutdown() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Boss event loop group shutdowned.");
		}

		if (workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Worker event loop group shutdowned.");
		}

		logger.debug("Menton HTTP server stopped.");
		inform();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.general.TaskCompletionInformer#register(net.anyflow.
	 * menton.general.TaskCompletionListener)
	 */
	@Override
	public void register(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.add(taskCompletionListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.general.TaskCompletionInformer#deregister(net.anyflow.
	 * menton.general.TaskCompletionListener)
	 */
	@Override
	public void deregister(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.remove(taskCompletionListener);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.general.TaskCompletionInformer#inform()
	 */
	@Override
	public void inform() {
		for (TaskCompletionListener taskCompletionListener : taskCompletionListeners) {
			taskCompletionListener.taskCompleted(this, false);
		}
	}
}