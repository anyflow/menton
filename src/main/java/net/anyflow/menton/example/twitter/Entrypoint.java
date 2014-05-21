package net.anyflow.menton.example.twitter;

import net.anyflow.menton.Configurator;
import net.anyflow.menton.general.TaskCompletionListener;
import net.anyflow.menton.http.HttpServer;

/**
 * Process entrypoint which contains main function.
 * 
 * @author anyflow
 */
public class Entrypoint implements TaskCompletionListener {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Entrypoint.class);

	private final HttpServer httpServer = new HttpServer();

	public void start() {

		try {
			org.apache.log4j.PropertyConfigurator.configure(getClass().getClassLoader().getResourceAsStream("META-INF/example/twitter/log4j.properties"));

			logger.info("Starting Twitter...");

			net.anyflow.menton.Configurator.instance().initialize(getClass().getClassLoader().getResourceAsStream("META-INF/example/twitter/application.properties"));

			// HTTP server initiation.
			httpServer.start(null);

			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					shutdown(true);
				}
			});
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(-1);
		}
	}

	public int port() {
		String port = Configurator.instance().getProperty("menton.httpServer.port");
		
		try {
			return Integer.parseInt(port);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}
	
	public void shutdown(boolean haltJavaRuntime) {

		httpServer.shutdown();

		logger.info("Twitter shutdowned gracefully.");

		if(haltJavaRuntime) {
			Runtime.getRuntime().halt(0);
		}
	}

	@Override
	public void taskCompleted(Object worker, boolean furtherTaskingAvailable) {
	}
}