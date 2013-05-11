package net.anyflow.menton.http;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.exception.DefaultException;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private static ServerBootstrap bootstrap;

	public HttpServer() {
	}

	public static void start(final ChannelHandler channelHandler) throws DefaultException {
		start(channelHandler, Configurator.getHttpPort());
	}

	public static void start(final ChannelHandler channelHandler, int port) {

		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				// Create a default pipeline implementation.
				ChannelPipeline pipeline = org.jboss.netty.channel.Channels.pipeline();

				// Uncomment the following line if you want HTTPS
				// SSLEngine engine =
				// SecureChatSslContextFactory.getServerContext().createSSLEngine();
				// engine.setUseClientMode(false);
				// pipeline.addLast("ssl", new SslHandler(engine));

				pipeline.addLast("decoder", new HttpRequestDecoder());
				// Uncomment the following line if you don't want to handle
				// HttpChunks.
				// pipeline.addLast("aggregator", new
				// HttpChunkAggregator(1048576));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				// Remove the following line if you don't want automatic content
				// compression.
				pipeline.addLast("deflater", new HttpContentCompressor());
				pipeline.addLast("handler", channelHandler);

				return pipeline;
			}
		});

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress(port));

		logger.info("Htttp server started.");
	}

	public static void stop() {
		if(bootstrap == null) { return; }

		bootstrap.releaseExternalResources();
	}
}