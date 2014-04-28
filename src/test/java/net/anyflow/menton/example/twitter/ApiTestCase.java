package net.anyflow.menton.example.twitter;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

@Ignore(value = "")
public class ApiTestCase {

	static boolean classTest = false;

	public static boolean classTest() {
		return classTest;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		if(!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
		}

		if(ApiTestSuite.server() != null) { return; }

		ApiTestSuite.setUp();
		classTest = true;
	}

	@AfterClass
	public static void afterClass() throws Exception {

		if(classTest == false) { return; }

		ApiTestSuite.tearDown();
	}
}