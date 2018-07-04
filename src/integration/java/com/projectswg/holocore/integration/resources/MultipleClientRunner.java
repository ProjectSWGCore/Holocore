package com.projectswg.holocore.integration.resources;

import com.projectswg.common.network.packets.swg.login.creation.DeleteCharacterRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultipleClientRunner {
	
	private final HolocoreClient [] clients;
	private final boolean deleteCharacters;
	
	public MultipleClientRunner(int clientCount, boolean deleteCharacters) {
		this.clients = new HolocoreClient[clientCount];
		this.deleteCharacters = deleteCharacters;
	}
	
	@Before
	public void initializeClient() {
		for (int i = 0; i < clients.length; i++) {
			clients[i] = new HolocoreClient();
			Assert.assertTrue(clients[i].login("Obique", "pass"));
		}
	}
	
	@After
	public void terminateClient() {
		for (HolocoreClient client : clients) {
			if (deleteCharacters)
				client.send(new DeleteCharacterRequest(0, client.getCharacterId()));
			client.disconnect();
		}
	}
	
	protected HolocoreClient client(int index) {
		return clients[index];
	}
	
}
