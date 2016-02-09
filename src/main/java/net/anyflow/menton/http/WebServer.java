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
import net.anyflow.menton.Settings;
import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

/**
 * @author anyflow
 */
public class WebServer implements TaskCompletionInformer {

	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private final List<TaskCompletionListener> taskCompletionListeners;

	public WebServer() {
		taskCompletionListeners = Lists.newArrayList();

		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("menton.system.bossThreadCount", 0),
				new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("menton.system.workerThreadCount", 0),
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
	public void start(String requestHandlerPakcageRoot, final WebsocketFrameHandler websocketFrameHandler) {
		HttpRequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
		try {
			if (Settings.SELF.httpPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(false, websocketFrameHandler));

				bootstrap.bind(Settings.SELF.httpPort()).sync();
			}

			if (Settings.SELF.httpsPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(true, websocketFrameHandler));

				bootstrap.bind(Settings.SELF.httpsPort()).sync();
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