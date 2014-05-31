package net.anyflow.menton.example.twitter.httphandler;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.util.Date;
import java.util.Map;

import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.HttpConstants.HeaderValues;
import net.anyflow.menton.http.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "PUT" })
public class Tweet_Put extends RequestHandler {

	@Override
	public String call() {
		Map<String, String> parameters = getParameters();

		String message = parameters.get("message");

		if(message == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);

			MessageGenerator.generateJson(new Error("Invalid message"), httpResponse());
		}

		Tweet tweet = new Tweet();

		tweet.setMessage(message);
		tweet.setDate(new Date());

		String id = Database.instance().insert(tweet);

		try {
			JSONObject json = new JSONObject();

			json.put("id", id);
			return json.toString();
		}
		catch(JSONException e) {
			httpResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return null;
		}
	}

	Map<String, String> getParameters() {
		Map<String, String> ret = Maps.newHashMap();

		if(httpRequest().headers().contains(Names.CONTENT_TYPE) == false
				|| httpRequest().headers().get(Names.CONTENT_TYPE).equals(Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
			ret.put("message", httpRequest().parameter("message"));
		}
		else if(httpRequest().headers().get(Names.CONTENT_TYPE).equals(HeaderValues.APPLICATION_JSON)) {
			String json = httpRequest().content().toString(CharsetUtil.UTF_8);

			ret.put("message", JsonPath.read(json, "$.message").toString());
		}

		return ret;
	}
}