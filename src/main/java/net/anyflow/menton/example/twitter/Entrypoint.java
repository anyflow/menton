package net.anyflow.menton.example.twitter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import net.anyflow.menton.general.TaskCompletionListener;
import net.anyflow.menton.http.HttpServer;

/**
 * Process entrypoint which contains main function.
 * 
 * @author anyflow
 */
public class Entrypoint implements TaskCompletionListener {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Entrypoint.class);
	private static Entrypoint instance;

	private final HttpServer httpServer = new HttpServer();

	/**
	 * @param args
	 * @throws URISyntaxException
	 * @throws DefaultException
	 * @throws IOException
	 */
	public static void main(String[] args) throws URISyntaxException, IOException {
		Entrypoint.instance().start();
	}

	public static Entrypoint instance() {
		if(instance == null) {
			instance = new Entrypoint();
		}

		return instance;
	}

	private Entrypoint() {
	}

	private void start() throws FileNotFoundException {

		String log4jFilePath = "/META-INF/log4j.properties";
		String applicationPropertyFilePath = "/META-INF/application.properties";

		org.apache.log4j.PropertyConfigurator.configure(getClass().getResourceAsStream(log4jFilePath));

		logger.info("Starting Twitter...");

		net.anyflow.menton.Configurator.instance().initialize(getClass().getResourceAsStream(applicationPropertyFilePath));

		// HTTP server initiation.
		httpServer.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				shutdown(true);
			}
		});
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