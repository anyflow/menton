/**
 * 
 */
package net.anyflow.menton.example.twitter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.anyflow.menton.example.twitter.model.Tweet;

/**
 * @author Park Hyunjeong
 */
public class Database {

	private static Database instance;

	public static Database instance() {
		if(instance != null) { return instance; }

		instance = new Database();
		return instance;
	}

	Map<String, Tweet> store = new HashMap<String, Tweet>();

	public Tweet get(String id) {
		return store.get(id);
	}

	public String insert(Tweet tweet) {
		tweet.setId(UUID.randomUUID().toString());

		store.put(tweet.getId(), tweet);

		return tweet.getId();
	}

	public Tweet update(Tweet tweet) {
		Tweet ret = store.remove(tweet.getId());

		store.put(tweet.getId(), tweet);

		return ret;
	}

	public Tweet delete(String id) {
		return store.remove(id);
	}

	public Collection<Tweet> list() {
		return store.values();
	}
}
