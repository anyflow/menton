package net.anyflow.menton.example.twitter.httphandler;

import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.menton.example.twitter.Database;
import net.anyflow.menton.example.twitter.MessageGenerator;
import net.anyflow.menton.example.twitter.model.Tweet;
import net.anyflow.menton.http.HttpConstants.HeaderValues;
import net.anyflow.menton.http.HttpRequestHandler;

/**
 * @author Park Hyunjeong
 */
@HttpRequestHandler.Handles(paths = { "twitter/tweet" }, httpMethods = { "POST" })
public class Tweet_Post extends HttpRequestHandler {

	@Override
	public String service() {
		String id, message;

		if (HeaderValues.APPLICATION_JSON.equals(httpRequest().headers().get(Names.CONTENT_TYPE))) {
			String content = httpRequest().content().toString(CharsetUtil.UTF_8);

			id = JsonPath.read(content, "$.id").toString();
			message = JsonPath.read(content, "$.message").toString();
		}
		else {
			id = httpRequest().parameter("id");
			message = httpRequest().parameter("message");
		}

		if (message == null || id == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);

			MessageGenerator.generateJson("Invalid parameter(s)", httpResponse());
		}

		Tweet tweet = Database.instance().get(id);
		if (tweet == null) {
			httpResponse().setStatus(HttpResponseStatus.FORBIDDEN);
			return MessageGenerator.generateJson("Invalid id", httpResponse());
		}

		tweet.setMessage(message);
		Database.instance().update(tweet);

		return MessageGenerator.generateJson(tweet, httpResponse());

	}
}