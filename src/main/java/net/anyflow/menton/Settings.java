/**
 * 
 */
package net.anyflow.menton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Maps;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Settings extends java.util.Properties {

	private static final long serialVersionUID = 5431592702381235221L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Settings.class);
	public final static Settings SELF;

	static {
		SELF = new Settings();
	}

	private Map<String, String> webResourceExtensionToMimes;
	private SelfSignedCertificate ssc;

	private Settings() {
		try {
			ssc = new SelfSignedCertificate();
		}
		catch (CertificateException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public int getInt(String key, int defaultValue) {
		String valueString = this.getProperty(key);

		if (valueString == null) { return defaultValue; }

		try {
			return Integer.parseInt(valueString);
		}
		catch (NumberFormatException e) {
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
			if (propertyReader != null) {
				load(propertyReader);
			}
			else if (propertyInputStream != null) {
				load(propertyInputStream);
			}
		}
		catch (java.io.IOException e) {
			logger.error("Loading network properties failed.", e);
			throw e;
		}

		webResourceExtensionToMimes = Maps.newHashMap();

		try {
			JSONObject obj = new JSONObject(getProperty("menton.httpServer.MIME"));
			@SuppressWarnings("unchecked")
			Iterator<String> keys = obj.keys();

			while (keys.hasNext()) {
				String key = keys.next();
				webResourceExtensionToMimes.put(key, obj.get(key).toString());
			}
		}
		catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Map<String, String> webResourceExtensionToMimes() {
		return webResourceExtensionToMimes;
	}

	public static Integer tryParse(String text) {
		try {
			return Integer.parseInt(text);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @return HTTP port. If empty, returns null(the channel will not be
	 *         established).
	 */
	public Integer httpPort() {
		return tryParse(getProperty("menton.httpServer.http.port", null));
	}

	/**
	 * @return HTTPS port. If empty, returns null(the channel will not be
	 *         established).
	 */
	public Integer httpsPort() {
		return tryParse(getProperty("menton.httpServer.https.port", null));
	}

	public File certChainFile() {
		String certFilePath = getProperty("menton.httpServer.https.certChainFilePath", null);

		return "self".equalsIgnoreCase(certFilePath) ? ssc.certificate() : new File(certFilePath);
	}

	public File privateKeyFile() {
		String privateKeyFilePath = getProperty("menton.httpServer.https.privateKeyFilePath", null);

		return "self".equalsIgnoreCase(privateKeyFilePath) ? ssc.privateKey() : new File(privateKeyFilePath);
	}

	/**
	 * @return context root path
	 */
	public String httpContextRoot() {
		String ret = getProperty("menton.httpServer.contextRoot", "/");

		if (ret.equalsIgnoreCase("") || ret.charAt(ret.length() - 1) != '/') {
			ret += "/";
		}

		return ret;
	}

	public List<String> websocketSubprotocols() {
		throw new RuntimeException("not Implemented");
	}

	public String WebResourcePhysicalRootPath() {
		return this.getProperty("menton.httpServer.webResourcePhysicalRootPath", null);
	}

	public void setWebResourcePhysicalRootPath(String physicalRootPath) {
		this.setProperty("menton.httpServer.webResourcePhysicalRootPath", physicalRootPath);
	}

	public LogLevel logLevel() {
		if (logger.isTraceEnabled()) {
			return LogLevel.TRACE;
		}
		else if (logger.isDebugEnabled()) {
			return LogLevel.DEBUG;
		}
		else if (logger.isInfoEnabled()) {
			return LogLevel.INFO;
		}
		else if (logger.isWarnEnabled()) {
			return LogLevel.WARN;
		}
		else {
			return LogLevel.DEBUG;
		}
	}
}