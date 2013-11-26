package net.anyflow.menton.avro;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Server;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroServer implements TaskCompletionInformer {

	private static final Logger logger = LoggerFactory.getLogger(AvroServer.class);
	
	private Server server;
	private List<TaskCompletionListener> taskCompletionListeners;
	
	public AvroServer() {
		taskCompletionListeners = new ArrayList<TaskCompletionListener>();
	}

	public void start(Responder responder) {
		start(responder, Configurator.instance().getAvroPort());
	}

	public void start(Responder responder, int port) {
		ExecutorService executor = Executors.newCachedThreadPool();

		server = new NettyServer(responder, new InetSocketAddress(port), new NioServerSocketChannelFactory(executor, executor), new ExecutionHandler(
				new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));

		logger.info("Avro server started.");
	}

	public void shutdown() {
		server.close();
		logger.debug("AvroServer closed.");
		
		inform();
	}

	public int getPort() {
		return server.getPort();
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