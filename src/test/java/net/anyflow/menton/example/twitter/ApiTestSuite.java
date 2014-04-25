package net.anyflow.menton.example.twitter;

import junit.framework.TestSuite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({})
public class ApiTestSuite extends TestSuite {

	public static boolean SERVER_ACTIVATED = false;

	@BeforeClass
	public static void setUp() throws Exception {
		net.anyflow.menton.example.twitter.Entrypoint.main(null);
		SERVER_ACTIVATED = true;
	}

	@AfterClass
	public static void tearDown() throws Exception {
		net.anyflow.menton.example.twitter.Entrypoint.instance().shutdown(false);
		SERVER_ACTIVATED = false;
	}
}