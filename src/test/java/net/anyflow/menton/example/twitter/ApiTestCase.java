package net.anyflow.menton.example.twitter;

import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

@Ignore(value = "")
public class ApiTestCase {

	private static HashMap<String, String> serverDomains;

	public static String SERVER_DOMAIN;

	static {
		if(serverDomains == null) {
			serverDomains = new HashMap<String, String>();

			serverDomains.put("localhost", "http://localhost:8001");

			String targetServer = System.getProperty("targetServer");
			if(targetServer == null || targetServer.equals("")) {
				targetServer = "localhost";
			}

			SERVER_DOMAIN = serverDomains.get(targetServer);
		}
	}

	static boolean classTest = false;

	public static boolean classTest() {
		return classTest;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		if(!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
		}

		if(ApiTestSuite.SERVER_ACTIVATED) { return; }

		ApiTestSuite.setUp();
		classTest = true;
	}

	@AfterClass
	public static void afterClass() throws Exception {

		if(classTest == false) { return; }

		ApiTestSuite.tearDown();
	}
}