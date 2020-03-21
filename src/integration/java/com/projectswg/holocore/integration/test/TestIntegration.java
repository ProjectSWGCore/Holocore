package com.projectswg.holocore.integration.test;

import com.projectswg.holocore.ProjectSWGRunner;
import com.projectswg.holocore.integration.test.login.TestLogin;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(value = {
		TestLogin.class
})
public class TestIntegration {
	
	private static final ProjectSWGRunner HOLOCORE = new ProjectSWGRunner();
	
	@BeforeClass
	public static void startHolocore() {
		HOLOCORE.start();
	}
	
	@AfterClass
	public static void stopHolocore() {
		HOLOCORE.stop();
	}
	
}
