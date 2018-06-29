package com.projectswg.holocore.integration.test.login;

import com.projectswg.common.network.packets.swg.login.creation.DeleteCharacterRequest;
import com.projectswg.holocore.integration.resources.ClientRunner;
import com.projectswg.holocore.integration.resources.ClientUtilities;
import org.junit.Test;

public class TestCharacterCreation extends ClientRunner {
	
	@Test
	public void testCreateCharacter() {
		ClientUtilities.createCharacter(client);
		System.out.println("Created character: " + client.getCharacterName() + " with id " + client.getCharacterId());
		client.waitForZoneIn();
		System.out.println("Zoned in");
		client.send(new DeleteCharacterRequest(0, client.getCharacterId()));
	}
	
}
