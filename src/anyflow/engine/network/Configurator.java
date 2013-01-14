/**
 * 
 */
package anyflow.engine.network;

import java.net.URISyntaxException;

import anyflow.engine.network.exception.DefaultException;

/**
 * @author anyflow
 *
 */
public class Configurator {
	
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configurator.class);
	private static java.util.Properties configuration;
	
	/**
	 * initialize the configurator with the default properties file which should be located in the same path with the jar file.
	 */
	public static void initialize() {
		try {
			String configPath = (new java.io.File(Configurator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())).getParentFile().getPath();
			initialize(configPath + "/facade.properties");
		}
		catch (URISyntaxException e) {
			logger.error("Getting the configuration file path has failed.", e);
		}	
	}
	
	/**
	 * initialize the configurator
	 * @param propertyFilePath full path and file name of the properties file.
	 */
	public static void initialize(String propertyFilePath) {
		
		if(configuration == null) {
			configuration = new java.util.Properties();
		}
		
		try {
			java.io.InputStream is = new java.io.FileInputStream(propertyFilePath);
			
			configuration.load(is);
		}
		catch (java.io.FileNotFoundException e) {
			logger.error("Error occured on finding configuration file path.", e);
		}
		catch (java.io.IOException e) {
			logger.error("Loading network properties failed.", e);
		}
	}
	
	/**
	 * @return Service package name
	 * @throws DefaultException
	 */
	public static String getServicePackageName() throws DefaultException {
		if(configuration == null) { throw new DefaultException("Configurator.configure should be called before this."); } 
		
		return configuration.getProperty("httpServer.servicePackageName", "package name was not found");
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
}