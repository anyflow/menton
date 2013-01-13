package anyflow.engine.network;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Server;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anyflow.engine.network.exception.DefaultException;


public class AvroServer {
	
	private static final Logger logger = LoggerFactory.getLogger(AvroServer.class);
	
	private static AvroServer instance;
	
	private Server server;
	
	private AvroServer() {
	}
	
	public static void start(Responder responder) throws DefaultException {
		start(responder, Configurator.getAvroPort());
	}
	
	public static void start(Responder responder, int port) {
		if(instance == null) {
			instance = new AvroServer();
		}

		ExecutorService executor = Executors.newCachedThreadPool();
		
		instance.server = new NettyServer(responder
 					    , new InetSocketAddress(port)
					    , new NioServerSocketChannelFactory(executor
					    								  , executor)
					    , new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));

		logger.info("Avro server started.");
	}
	
	public static void stop() {
		if(instance == null) { 
			return;
		}
		
		instance.server.close();
	}
	
	public static int getPort() {
		if(instance == null) { 
			return -1;
		}
		
		return instance.server.getPort();
	}
}