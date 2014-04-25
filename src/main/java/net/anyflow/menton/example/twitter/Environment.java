package net.anyflow.menton.example.twitter;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * @author anyflow
 */
public class Environment {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Environment.class);

	public static <T> String getWorkingPath(java.lang.Class<T> mainClass) {
		CodeSource codeSource = mainClass.getProtectionDomain().getCodeSource();

		try {
			return (new File(codeSource.getLocation().toURI().getPath())).getParentFile().getPath();
		}
		catch(URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}