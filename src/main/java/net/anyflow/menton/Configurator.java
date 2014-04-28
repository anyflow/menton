/**
 * 
 */
package net.anyflow.menton;

import io.netty.handler.logging.LogLevel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author anyflow
 */
public class Configurator extends java.util.Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5431592702381235221L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configurator.class);
	private static Configurator instance;
	
	public static Configurator instance() {
		if(instance == null) {
			instance = new Configurator();
		}

		return instance;
	}

	public int getInt(String key, int defaultValue) {
		String valueString = this.getProperty(key);

		if(valueString == null) { return defaultValue; }

		try {
			return Integer.parseInt(valueString);
		}
		catch(NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * initialize the configurator
	 * 
	 * @param propertyInputStream
	 *            Menton's properties InputStream
	 * @throws IOException 
	 */
	public void initialize(java.io.InputStream propertyInputStream) throws IOException {		
		initialize(null, propertyInputStream);
	}

	private void initialize(Reader propertyReader, InputStream propertyInputStream) throws IOException {

		try {
			if(propertyReader != null) {
				load(propertyReader);
			}
			else if(propertyInputStream != null) {
				load(propertyInputStream);
			}
		}
		catch(java.io.IOException e) {
			logger.error("Loading network properties failed.", e);
			throw e;
		}
	}
	
	public void initialize(Reader propertyReader) throws IOException {
		initialize(propertyReader, null);
	}

	/**
	 * @return http port
	 */
	public int httpPort() {
		return Integer.parseInt(getProperty("menton.httpServer.port", "8090"));
	}

	/**
	 * @return context root path
	 */
	public String httpContextRoot() {
		String ret = getProperty("menton.httpServer.contextRoot", "/");

		if(ret.equalsIgnoreCase("") || ret.charAt(ret.length() - 1) != '/') {
			ret += "/";
		}

		return ret;
	}

	public LogLevel logLevel() {
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