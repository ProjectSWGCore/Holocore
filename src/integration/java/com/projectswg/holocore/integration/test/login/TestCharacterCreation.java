package com.projectswg.holocore.integration.test.login;

import com.projectswg.common.network.packets.swg.login.creation.DeleteCharacterRequest;
import com.projectswg.holocore.integration.resources.ClientRunner;
import com.projectswg.holocore.integration.resources.ClientUtilities;
import org.junit.Test;

public class TestCharacterCreation extends ClientRunner {
	
	@Test
	public void testCreateCharacter() {
		try {
			ClientUtilities.createCharacter(client);
			client.waitForZoneIn();
		} finally {
			client.send(new DeleteCharacterRequest(0, client.getCharacterId()));
		}
	}
	
}
