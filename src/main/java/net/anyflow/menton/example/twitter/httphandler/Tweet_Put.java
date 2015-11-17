package net.anyflow.menton.example.twitter.httphandler;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.HttpConstants.HeaderValues;
import net.anyflow.menton.http.RequestHandler;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "PUT" })
public class Tweet_Put extends RequestHandler {

	@Override
	public String call() {
		String message;

		if(HeaderValues.APPLICATION_JSON.equals(httpRequest().headers().get(Names.CONTENT_TYPE))) {
			String content = httpRequest().content().toString(CharsetUtil.UTF_8);
			message = JsonPath.read(content, "$.message").toString();
		}
		else {
			message = httpRequest().parameter("message");
		}

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
}