/**
 * 
 */
package net.anyflow.menton;

import io.netty.handler.logging.LogLevel;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

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
	 */
	public void initialize(java.io.InputStream propertyInputStream) {		
		initialize(null, propertyInputStream);
	}

	private void initialize(Reader propertyReader, InputStream propertyInputStream) {

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
		}
		
		Properties prop = new Properties();

		try {
			String filePath = "META-INF/maven/net.anyflow/menton/pom.properties";
			InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
			
			prop.load(in);
			in.close();
		}
		catch(Exception e) {
			logger.error("Failed to read pom.properties.", e);
		}
		
		for(Object key : prop.keySet()) {
			this.put(key, prop.values());
		}
	}
	
	public void initialize(Reader propertyReader) {
		initialize(propertyReader, null);
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
		return Integer.parseInt(getProperty("menton.httpServer.port", "8090"));
	}

	/**
	 * @return context root path
	 */
	public String getHttpContextRoot() {
		String ret = getProperty("menton.httpServer.contextRoot", "/");

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