package net.anyflow.menton.example.twitter.httphandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.RequestHandler;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "POST" })
public class Tweet_Post extends RequestHandler {

	@Override
	public String call() {
		String id = httpRequest().parameter("id");
		String message = httpRequest().parameter("message");

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

		return null;
	}
}