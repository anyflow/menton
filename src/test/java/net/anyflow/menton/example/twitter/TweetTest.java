package net.anyflow.menton.example.twitter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.menton.Configurator;
import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpConstants.HeaderValues;
import net.anyflow.menton.http.HttpResponse;
import net.anyflow.menton.http.IHttpClient;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TweetTest extends ApiTestCase {

	@BeforeClass
	public static void setup() {

	}

	@AfterClass
	public static void teardown() {

	}

	final String address = "http://localhost:" + Configurator.instance().httpPort() + "/twitter/tweet";
	static String tweetId = null;
	static String message = "Hello, menton-Twitter!";

	@Test
	public void test1_PUT() throws UnsupportedOperationException, URISyntaxException {

		IHttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("message", message);

		HttpResponse response = client.put();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		assertThat(content, containsString("id"));

		tweetId = JsonPath.read(content, "$.id");
	}

	@Test
	public void test2_PUT_JSON() throws UnsupportedOperationException, URISyntaxException, JSONException {

		IHttpClient client = new HttpClient(address);
		client.httpRequest().headers().set(Names.CONTENT_TYPE, HeaderValues.APPLICATION_JSON);

		JSONObject obj = new JSONObject();
		obj.put("message", "another messsage");

		client.httpRequest().setContent(obj.toString());

		HttpResponse response = client.put();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		assertThat(content, containsString("id"));
	}

	@Test
	public void test3_GET() throws UnsupportedOperationException, URISyntaxException {

		IHttpClient client = new HttpClient(address + "/" + tweetId);
		HttpResponse response = client.get();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		assertThat(JsonPath.read(content, "$.id").toString(), is(tweetId));
		assertThat(JsonPath.read(content, "$.message").toString(), is(message));
	}

	@Test
	public void test4_POST() throws UnsupportedOperationException, URISyntaxException {

		String updatedMessage = "updated message";

		IHttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);
		client.httpRequest().addParameter("message", updatedMessage);

		HttpResponse response = client.post();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		assertThat(JsonPath.read(content, "$.id").toString(), is(tweetId));
		assertThat(JsonPath.read(content, "$.message").toString(), is(updatedMessage));
	}

	@Test
	public void test5_POST_JSON() throws UnsupportedOperationException, URISyntaxException, JSONException {

		String updatedMessage = "the second updated message";

		IHttpClient client = new HttpClient(address);
		client.httpRequest().headers().set(Names.CONTENT_TYPE, HeaderValues.APPLICATION_JSON);

		JSONObject obj = new JSONObject();
		obj.put("id", tweetId);
		obj.put("message", updatedMessage);

		client.httpRequest().setContent(obj.toString());

		HttpResponse response = client.post();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		assertThat(JsonPath.read(content, "$.id").toString(), is(tweetId));
		assertThat(JsonPath.read(content, "$.message").toString(), is(updatedMessage));
	}

	@Test
	public void test6_DELETE() throws UnsupportedOperationException, URISyntaxException {

		IHttpClient client = new HttpClient(address);
		client.httpRequest().addParameter("id", tweetId);

		HttpResponse response = client.delete();

		assertThat(response.getStatus(), is(HttpResponseStatus.OK));

		client = new HttpClient(address + "/" + tweetId);

		response = client.get();

		assertThat(response.getStatus(), is(HttpResponseStatus.FORBIDDEN));
	}
}