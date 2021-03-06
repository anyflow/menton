package net.anyflow.menton.example.twitter.httphandler;

import java.util.Collection;

import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.HttpRequestHandler;

/**
 * @author Park Hyunjeong
 */
@HttpRequestHandler.Handles(paths = { "twitter/list" }, httpMethods = { "GET" })
public class List extends HttpRequestHandler {

	@Override
	public String service() {
		Collection<Tweet> tweets = Database.instance().list();

		return MessageGenerator.generateJson(tweets, httpResponse());
	}
}