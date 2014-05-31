package net.anyflow.menton.example.twitter;

import static org.junit.Assert.assertTrue;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.net.URISyntaxException;
import java.util.List;

import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jayway.jsonpath.JsonPath;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListTest extends ApiTestCase {

	@BeforeClass
	public static void setup() throws UnsupportedOperationException, URISyntaxException {
		TweetTest tt = new TweetTest();

		for(int i = 0; i < count; ++i) {
			tt.test1_PUT();
		}
	}

	@AfterClass
	public static void teardown() {

	}

	final String address = "http://localhost:" + ApiTestSuite.server().port() + "/twitter/list";
	final static int count = 100;

	@Test
	public void test() throws UnsupportedOperationException, URISyntaxException {

		HttpClient client = new HttpClient(address);

		HttpResponse response = client.get();

		assertTrue(response.getStatus().equals(HttpResponseStatus.OK));

		String content = response.content().toString(CharsetUtil.UTF_8);

		List<Object> list = JsonPath.read(content, "$.");

		assertTrue(list.size() >= count);
	}
}