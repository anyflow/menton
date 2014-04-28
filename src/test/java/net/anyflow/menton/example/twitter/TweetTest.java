package net.anyflow.menton.example.twitter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.net.URISyntaxException;

import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jayway.jsonpath.JsonPath;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TweetTest extends ApiTestCase {

	@BeforeClass
	public static void setup() {

	}

	@AfterClass
	public static void teardown() {

	}

	final String address = "http://localhost:" + ApiTestSuite.server().port() + "/twitter/tweet";
	static String tweetId = null;
	static String message = "Hello, menton-Twitter!";
	
	@Test
	public void test1_PUT() throws UnsupportedOperationException, URISyntaxException {

		HttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("message", message);

		HttpResponse response = client.put();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));
		
		String content = response.content().toString(CharsetUtil.UTF_8);
		
		assertThat(content, containsString("id"));
		
		tweetId = JsonPath.read(content, "$.id");
	}
	
	@Test
	public void test2_GET() throws UnsupportedOperationException, URISyntaxException {

		HttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);
		HttpResponse response = client.get();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);
		
		assertThat(JsonPath.read(content, "$.id").toString(), is(tweetId));
		assertThat(JsonPath.read(content, "$.message").toString(), is(message));
	}
	
	@Test
	public void test3_UPDATE() throws UnsupportedOperationException, URISyntaxException {

		String updatedMessage = "updated message";
		
		HttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);
		client.httpRequest().addParameter("message", updatedMessage);
		
		HttpResponse response = client.post();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));
		
		String content = response.content().toString(CharsetUtil.UTF_8);
		
		assertThat(JsonPath.read(content, "$.id").toString(), is(tweetId));
		assertThat(JsonPath.read(content, "$.message").toString(), is(updatedMessage));		
	}
	
	@Test
	public void test4_DELETE() throws UnsupportedOperationException, URISyntaxException {

		HttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);
		
		HttpResponse response = client.delete();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));
		
		client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);
		
		response = client.get();

		assertThat(response.getStatus(), is(HttpResponseStatus.FORBIDDEN));
	}
}