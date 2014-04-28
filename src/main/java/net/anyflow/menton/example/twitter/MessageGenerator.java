/**
 * 
 */
package net.anyflow.menton.example.twitter;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;

import net.anyflow.menton.http.HttpResponse;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author Park Hyunjeong
 */
public class MessageGenerator {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageGenerator.class);
	
	public static String generateJson(Object value, HttpResponse response) {
		try {
			return (new ObjectMapper()).writer().writeValueAsString(value);
		}
		catch(IOException e) {
			logger.error(e.getMessage(), e);
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return null;
		}
	}
}