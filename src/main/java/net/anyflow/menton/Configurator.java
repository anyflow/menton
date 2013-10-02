/**
 * 
 */
package net.anyflow.menton;

import io.netty.handler.logging.LogLevel;

/**
 * @author anyflow
 */
public class Configurator extends java.util.Properties {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configurator.class);
	private static Configurator instance;

	public static Configurator instance() {
		if(instance == null) {
			instance = new Configurator();
		}

		return instance;
	}

	/**
	 * initialize the configurator
	 * 
	 * @param propertyInputStream
	 *            Menton's properties InputStream
	 */
	public void initialize(java.io.InputStream propertyInputStream) {
		try {
			load(propertyInputStream);
		}
		catch(java.io.IOException e) {
			logger.error("Loading network properties failed.", e);
		}
	}

	public void setRequestHandlerPackageRoot(String requestHandlerPackageRoot) {
		setProperty("httpServer.requestHandlerPackageRoot", requestHandlerPackageRoot);
	}

	/**
	 * @return Request Handler Package root name
	 */
	public String getRequestHandlerPackageRoot() {
		return getProperty("httpServer.requestHandlerPackageRoot", "requestHandler Package key was not found");
	}

	/**
	 * @return http port
	 */
	public int getHttpPort() {
		return Integer.parseInt(getProperty("httpServer.port", "8090"));
	}

	/**
	 * @return avro port
	 */
	public int getAvroPort() {
		return Integer.parseInt(getProperty("avroServer.port", "9090"));
	}

	/**
	 * @return context root path
	 */
	public String getHttpContextRoot() {
		String ret = getProperty("httpServer.contextRoot", "/");

		if(ret.equalsIgnoreCase("") || ret.charAt(ret.length() - 1) != '/') {
			ret += "/";
		}

		return ret;
	}

	public LogLevel getLogLevel() {
		if(logger.isTraceEnabled()) {
			return LogLevel.TRACE;
		}
		else if(logger.isDebugEnabled()) {
			return LogLevel.DEBUG;
		}
		else if(logger.isInfoEnabled()) {
			return LogLevel.INFO;
		}
		else if(logger.isWarnEnabled()) {
			return LogLevel.WARN;
		}
		else {
			return LogLevel.DEBUG;
		}
	}
}