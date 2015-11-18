package net.anyflow.menton.example.twitter.httphandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.RequestHandler;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "DELETE" })
public class Tweet_Delete extends RequestHandler {

	@Override
	public String service() {
		String id = httpRequest().parameter("id");
		if (id == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);
			return MessageGenerator.generateJson(new Error("Invalid id"), httpResponse());
		}

		Tweet tweet = Database.instance().delete(id);
		if (tweet == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);
			return MessageGenerator.generateJson(new Error("Invalid id"), httpResponse());
		}

		return null;
	}
}