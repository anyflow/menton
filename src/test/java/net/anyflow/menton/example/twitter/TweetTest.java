package net.anyflow.menton.example.twitter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.URISyntaxException;

import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TweetTest extends ApiTestCase {

	@BeforeClass
	public static void setup() {

	}

	@AfterClass
	public static void teardown() {

	}

	@Test
	public void testPut() throws UnsupportedOperationException, URISyntaxException {

		HttpClient client = new HttpClient("http://localhost:8001/twitter/tweet");
		client.httpRequest().addParameter("message", "Hello, menton-Twitter!");

		HttpResponse response = client.put();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));
	}
}