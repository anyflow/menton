package net.anyflow.menton.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.List;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpServer implements TaskCompletionInformer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private List<TaskCompletionListener> taskCompletionListeners;
	
	public HttpServer() {
		taskCompletionListeners = new ArrayList<TaskCompletionListener>();
	}

	public void start(String requestHandlerPackageRoot) {
		start(requestHandlerPackageRoot, null, Configurator.instance().getHttpPort());
	}

	public void start(Class<? extends RequestHandler> requestHandlerClass) {
		start(null, requestHandlerClass, Configurator.instance().getHttpPort());
	}

	public void start(String requestHandlerPackageRoot, int port) {
		start(requestHandlerPackageRoot, null, port);
	}

	public void start(Class<? extends RequestHandler> requestHandlerClass, int port) {
		start(null, requestHandlerClass, port);
	}

	private void start(String requestHandlerPackageRoot, Class<? extends RequestHandler> requestHandlerClass, int port) {
		bossGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.bossThreadCount", 0), new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.workerThreadCount", 0), new DefaultThreadFactory("server/worker"));
		
		try {
			ServerChannelInitializer serverChannelInitializer = requestHandlerClass != null ? new ServerChannelInitializer(requestHandlerClass)
					: new ServerChannelInitializer(requestHandlerPackageRoot);

			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(serverChannelInitializer);
			bootstrap.bind(port).sync();

			logger.info("Menton HTTP server started.");
		}
		catch(InterruptedException e) {
			logger.error("Menton HTTP server failed to start...", e);
			shutdown();
		}
	}

	public void shutdown() {
		if(bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Boss event loop group shutdowned.");
		}

		if(workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Worker event loop group shutdowned.");
		}

		logger.debug("Menton HTTP server stopped.");
		inform();
	}

	/* (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#register(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void register(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.add(taskCompletionListener);
	}

	/* (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#deregister(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void deregister(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.remove(taskCompletionListener);
		
	}

	/* (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#inform()
	 */
	@Override
	public void inform() {
		for(TaskCompletionListener taskCompletionListener : taskCompletionListeners) {
			taskCompletionListener.taskCompleted(this, false);
		}
	}
}