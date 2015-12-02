package net.anyflow.menton.example.twitter;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.menton.Settings;
import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpResponse;
import net.anyflow.menton.http.IHttpClient;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListTest extends ApiTestCase {

	@BeforeClass
	public static void setup() throws UnsupportedOperationException, URISyntaxException {
		TweetTest tt = new TweetTest();

		for (int i = 0; i < count; ++i) {
			tt.test1_PUT();
		}
	}

	@AfterClass
	public static void teardown() {

	}

	final String address = "http://localhost:" + Settings.SELF.httpPort() + "/twitter/list";
	final static int count = 100;

	@Test
	public void test() throws UnsupportedOperationException, URISyntaxException {

		IHttpClient client = new HttpClient(address);

		HttpResponse response = client.get();

		assertTrue(response.getStatus().equals(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		List<Object> list = JsonPath.read(content, "$.");

		assertTrue(list.size() >= count);
	}
}