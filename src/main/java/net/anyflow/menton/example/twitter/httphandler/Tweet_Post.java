package net.anyflow.menton.example.twitter.httphandler;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.util.Map;

import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.HttpConstants.HeaderValues;
import net.anyflow.menton.http.RequestHandler;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "POST" })
public class Tweet_Post extends RequestHandler {

	@Override
	public String call() {
		Map<String, String> parameters = getParameters();

		String id = parameters.get("id");
		String message = parameters.get("message");

		if(message == null || id == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);

			MessageGenerator.generateJson("Invalid parameter(s)", httpResponse());
		}

		Tweet tweet = Database.instance().get(id);
		if(tweet == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);
			return MessageGenerator.generateJson("Invalid id", httpResponse());
		}

		tweet.setMessage(message);
		Database.instance().update(tweet);

		return MessageGenerator.generateJson(tweet, httpResponse());

	}

	Map<String, String> getParameters() {
		Map<String, String> ret = Maps.newHashMap();

		if(httpRequest().headers().contains(Names.CONTENT_TYPE) == false
				|| httpRequest().headers().get(Names.CONTENT_TYPE).equals(Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
			ret.put("id", httpRequest().parameter("id"));
			ret.put("message", httpRequest().parameter("message"));
		}
		else if(httpRequest().headers().get(Names.CONTENT_TYPE).equals(HeaderValues.APPLICATION_JSON)) {
			String json = httpRequest().content().toString(CharsetUtil.UTF_8);

			ret.put("id", JsonPath.read(json, "$.id").toString());
			ret.put("message", JsonPath.read(json, "$.message").toString());
		}

		return ret;
	}
}