/**
 * 
 */
package net.anyflow.menton;

import net.anyflow.menton.exception.DefaultException;

/**
 * @author anyflow
 */
public class Configurator {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configurator.class);
	private static java.util.Properties configuration;

	/**
	 * initialize the configurator
	 * 
	 * @param propertyFilePath
	 *            full path and file name of the properties file.
	 * @param httpRequestHandlerPackageRoot
	 *            null in case of no using http service.
	 */
	public static void initialize(String propertyFilePath) {

		if(configuration == null) {
			configuration = new java.util.Properties();
		}

		try {
			java.io.InputStream is = new java.io.FileInputStream(propertyFilePath);

			configuration.load(is);
		}
		catch(java.io.FileNotFoundException e) {
			logger.error("Error occured on finding configuration file path.", e);
		}
		catch(java.io.IOException e) {
			logger.error("Loading network properties failed.", e);
		}
	}

	public static void setRequestHandlerPackageRoot(String requestHandlerPackageRoot) throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); }

		configuration.setProperty("httpServer.requestHandlerPackageRoot", requestHandlerPackageRoot);
	}

	/**
	 * @return Request Handler Package root name
	 * @throws DefaultException
	 */
	public static String getRequestHandlerPackageRoot() throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); }

		return configuration.getProperty("httpServer.requestHandlerPackageRoot",
				"requestHandler Package key was not found");
	}

	/**
	 * @return http port
	 * @throws DefaultException
	 */
	public static int getHttpPort() throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); }

		return Integer.parseInt(configuration.getProperty("httpServer.port", "8090"));
	}

	/**
	 * @return avro port
	 * @throws DefaultException
	 */
	public static int getAvroPort() throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); }

		return Integer.parseInt(configuration.getProperty("avroServer.port", "9090"));
	}

	/**
	 * @return context root path
	 * @throws DefaultException
	 */
	public static String getHttpContextRoot() throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); }

		String ret = configuration.getProperty("httpServer.contextRoot", "/");

		if(ret.equalsIgnoreCase("") || ret.charAt(ret.length() - 1) != '/') {
			ret += "/";
		}

		return ret;
	}
}