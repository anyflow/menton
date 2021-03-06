package net.anyflow.menton.example.twitter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.TestSuite;

@RunWith(Suite.class)
@SuiteClasses({ TweetTest.class, ListTest.class })
public class ApiTestSuite extends TestSuite {

	private static net.anyflow.menton.example.twitter.Entrypoint server = null;

	@BeforeClass
	public static void setUp() throws Exception {
		server = new net.anyflow.menton.example.twitter.Entrypoint();
		server.start();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if (server == null) { return; }

		server.shutdown(false);
	}

	public static net.anyflow.menton.example.twitter.Entrypoint server() {
		return server;
	}
}