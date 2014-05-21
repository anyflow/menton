package net.anyflow.menton.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
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
	private final List<TaskCompletionListener> taskCompletionListeners;

	public HttpServer() {
		taskCompletionListeners = new ArrayList<TaskCompletionListener>();
	}

	/**
	 * @param requestHandlerClasses class type request handler list.
	 * @return
	 */
	public Channel start(List<Class<? extends RequestHandler>> requestHandlerClasses) {
		return start(requestHandlerClasses, null);
	}

	/**
	 * @param requestHandlerClasses class type request handler list.
	 * @param webSocketFrameHandler
	 * @return
	 */
	public Channel start(List<Class<? extends RequestHandler>> requestHandlerClasses, final WebSocketFrameHandler webSocketFrameHandler) {
		RequestHandler.setRequestHandlers(requestHandlerClasses);
		
		bossGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.bossThreadCount", 0), new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Configurator.instance().getInt("menton.system.workerThreadCount", 0), new DefaultThreadFactory(
				"server/worker"));

		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					// ChannelHandler adding order is 'very' important.
					// HttpServerHandler should be added last after outbound handlers in spite of it is inbound handler.
					// Otherwise, outbound handlers will not be handled.

					if("true".equalsIgnoreCase(Configurator.instance().getProperty("menton.logging.writelogOfNettyLogger"))) {
						ch.pipeline().addLast("log", new LoggingHandler("menton/server", Configurator.instance().logLevel()));
					}

					ch.pipeline().addLast("decoder", new HttpRequestDecoder());
					ch.pipeline().addLast("aggregator", new io.netty.handler.codec.http.HttpObjectAggregator(1048576)); // Handle HttpChunks.
					ch.pipeline().addLast("encoder", new HttpResponseEncoder());
					ch.pipeline().addLast("deflater", new HttpContentCompressor()); // Automatic content compression.
					ch.pipeline().addLast("bizHandler", new HttpServerHandler(webSocketFrameHandler));
				}
			});
			
			ChannelFuture channelFuture = bootstrap.bind(Configurator.instance().httpPort()).sync();

			logger.info("Menton HTTP server started.");

			return channelFuture.channel();
		}
		catch(InterruptedException e) {
			logger.error("Menton HTTP server failed to start...", e);
			shutdown();

			return null;
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

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#register(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void register(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.add(taskCompletionListener);
	}

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#deregister(net.anyflow.menton.general.TaskCompletionListener)
	 */
	@Override
	public void deregister(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.remove(taskCompletionListener);

	}

	/*
	 * (non-Javadoc)
	 * @see net.anyflow.menton.general.TaskCompletionInformer#inform()
	 */
	@Override
	public void inform() {
		for(TaskCompletionListener taskCompletionListener : taskCompletionListeners) {
			taskCompletionListener.taskCompleted(this, false);
		}
	}
}