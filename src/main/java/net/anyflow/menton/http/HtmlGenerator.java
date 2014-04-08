package net.anyflow.menton.http;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.anyflow.menton.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;

public class HtmlGenerator {

	static final Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);

	static String html_error;
	
	static {
		html_error = tidy(Thread.currentThread().getContextClassLoader().getResourceAsStream("html/error.htm"));
	}
	
	public static String generate(Map<String, String> values, String htmlPath) throws IOException {

		return replace(values, tidy(Thread.currentThread().getContextClassLoader().getResourceAsStream("html/error.htm")));
	}

	private static String replace(Map<String, String> values, String htmlTemplate) {

		String openMarker = "${";
		String closeMarker = "}";
		
		for(String key : values.keySet()) {
			htmlTemplate = htmlTemplate.replace(openMarker + key + closeMarker, values.get(key));
		}

		return htmlTemplate;
	}

	public static String error(String message, HttpResponseStatus status) {
		
		HashMap<String, String> values = new HashMap<String, String>();
		
		values.put("ERROR_CODE", status.toString());
		values.put("PROJECT_ARTIFACT_ID", Environment.PROJECT_ARTIFACT_ID);
		values.put("PROJECT_VERSION", Environment.PROJECT_VERSION);
		values.put("message", message);

		return replace(values, html_error);
	}
	
	private static String tidy(InputStream is) {

		Tidy tidy = new Tidy();

		tidy.setQuiet(true);
		tidy.setDocType("loose");
		tidy.setTidyMark(false);
		tidy.setOutputEncoding("UTF8");

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		tidy.parse(is, out);

		try {
			return out.toString("UTF-8");
		}
		catch(UnsupportedEncodingException e) {

			logger.error(e.getMessage(), e);
			return null;
		}
	}
}