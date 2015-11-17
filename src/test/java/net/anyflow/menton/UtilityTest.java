package net.anyflow.menton;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.http.QueryStringDecoder;
import net.anyflow.menton.http.HttpClient;

/**
 * @author anyflow
 */
public class UtilityTest {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UtilityTest.class);

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() {
		if(!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDown() {
	}

	@Test
	public void testQueryStringDecoderParameter() throws Exception {

		String queryString = "/asdfasd/qwerew";
		Map<String, List<String>> parameters = (new QueryStringDecoder(queryString)).parameters();

		assertThat(parameters, is(not(nullValue())));
	}

	@Test
	public void testURIport() throws Exception {

		String uriString = "http://10.0.0.1/getporttest";
		HttpClient client = new HttpClient(uriString);

		assertThat(client.httpRequest().uri().getPort(), is(80));
	}

	@Test
	public void testContentTypeWwwFormUrlEncodedWithUtf8() throws Exception {

		String uriString = "http://10.0.0.1/getporttest";
		HttpClient client = new HttpClient(uriString);

		assertThat(client.httpRequest().uri().getPort(), is(80));
	}

	@Test
	public void StringSplitTest() throws Exception {

		String testString = "img/404-not-found.png";

		String[] tokens = testString.split("\\.");
		assertThat(tokens[1], is("png"));
	}

	@Test
	public void StringMatchTest() throws Exception {

		String testString = "/session/{sessionId}/user/{userId}/";

		String[] tokens = testString.split("\\{\\w+\\}");

		for(int i = 0; i < tokens.length; ++i)
			System.out.println(tokens[i]);

		assertThat(tokens[0], is("/session/"));
	}
}