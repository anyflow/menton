/**
 * 
 */
package net.anyflow.menton;

import io.netty.handler.logging.LogLevel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Maps;

/**
 * @author anyflow
 */
public class Configurator extends java.util.Properties {

	private static final long serialVersionUID = 5431592702381235221L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configurator.class);
	private static Configurator instance;

	public static Configurator instance() {
		if(instance == null) {
			instance = new Configurator();
		}

		return instance;
	}

	private Map<String, String> webResourceExtensionToMimes;

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

	public void initialize(Reader propertyReader) throws IOException {
		initialize(propertyReader, null);
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

		webResourceExtensionToMimes = Maps.newHashMap();

		try {
			JSONObject obj = new JSONObject(getProperty("menton.httpServer.MIME"));
			@SuppressWarnings("unchecked")
			Iterator<String> keys = obj.keys();

			while(keys.hasNext()) {
				String key = keys.next();
				webResourceExtensionToMimes.put(key, obj.get(key).toString());
			}
		}
		catch(JSONException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Map<String, String> webResourceExtensionToMimes() {
		return webResourceExtensionToMimes;
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

	public String WebResourcePhysicalRootPath() {
		return this.getProperty("menton.httpServer.webResourcePhysicalRootPath", null);
	}

	public void setWebResourcePhysicalRootPath(String physicalRootPath) {
		this.setProperty("menton.httpServer.webResourcePhysicalRootPath", physicalRootPath);
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