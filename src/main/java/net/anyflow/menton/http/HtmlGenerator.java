package net.anyflow.menton.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;

public class HtmlGenerator {

	static final Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);

	public static String generate(Map<String, String> values, String htmlPath) throws IOException {

		String ret = tidy(Thread.currentThread().getContextClassLoader().getResourceAsStream(htmlPath));

		String open_marker = "${";
		String close_marker = "}";
		for(String key : values.keySet()) {
			ret = ret.replace(open_marker + key + close_marker, values.get(key));
		}

		return ret;
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