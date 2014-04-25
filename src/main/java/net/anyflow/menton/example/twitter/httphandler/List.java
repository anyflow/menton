package net.anyflow.menton.example.twitter.httphandler;

import java.util.Collection;

import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.RequestHandler;

/**
 * @author Park Hyunjeong
 */
@RequestHandler.Handles(paths = { "twitter/list" }, httpMethods = { "GET" })
public class List extends RequestHandler {

	@Override
	public String call() {
		Collection<Tweet> tweets = Database.instance().list();

		return MessageGenerator.generateJson(tweets, httpResponse());
	}
}