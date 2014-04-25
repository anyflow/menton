package net.anyflow.menton.example.twitter.httphandler;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Date;

import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "PUT" })
public class Tweet_Put extends RequestHandler {

	@Override
	public String call() {
		String message = httpRequest().parameter("message");
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

			json.put("tweet_id", id);
			return json.toString();
		}
		catch(JSONException e) {
			httpResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return null;
		}
	}
}