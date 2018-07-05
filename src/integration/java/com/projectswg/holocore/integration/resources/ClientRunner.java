package com.projectswg.holocore.integration.resources;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClientRunner {
	
	protected HolocoreClient client;
	
	@Before
	public void initializeClient() {
		client = new HolocoreClient();
		Assert.assertTrue(client.login("Obique", "pass"));
	}
	
	@After
	public void terminateClient() {
		client.disconnect();
	}
	
}
