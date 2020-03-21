package com.projectswg.holocore.integration.test.login;

import com.projectswg.holocore.integration.resources.ClientRunner;
import org.junit.Test;

public class TestExistingLogin extends ClientRunner {
	
	@Test
	public void testLoginExisting() {
		client.zoneIn(client.getRandomCharacter());
		client.waitForZoneIn();
	}
	
}
