/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.global.zone;

import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import org.junit.Assert;
import org.junit.Test;

public class TestZoneRequester extends TestRunnerNoIntents {
	
	@Test
	public void testNullCreatureObject() {
		ZoneRequester zr = new ZoneRequester();
		GenericPlayer player = new GenericPlayer();
		Assert.assertFalse(zr.onZoneRequested(null, player, 0));
		Assert.assertNotNull(player.getNextPacket(ErrorMessage.class));
	}
	
	@Test
	public void testInvalidCreatureObject() {
		ZoneRequester zr = new ZoneRequester();
		GenericPlayer player = new GenericPlayer();
		Assert.assertFalse(zr.onZoneRequested(new PlayerObject(1), player, 0));
		Assert.assertNotNull(player.getNextPacket(ErrorMessage.class));
	}
	
	@Test
	public void testNullPlayerObject() {
		ZoneRequester zr = new ZoneRequester();
		GenericCreatureObject creature = new GenericCreatureObject(getUniqueId());
		GenericPlayer player = creature.getOwner();
		creature.getSlottedObject("ghost").systemMove(null);
		Assert.assertFalse(zr.onZoneRequested(creature, player, creature.getObjectId()));
		Assert.assertNotNull(player.getNextPacket(ErrorMessage.class));
	}
	
	@Test
	public void testValidCreatureObject() {
		ZoneRequester zr = new ZoneRequester();
		GenericCreatureObject creature = new GenericCreatureObject(getUniqueId());
		GenericPlayer player = creature.getOwner();
		creature.setOwner(null);
		Assert.assertTrue(zr.onZoneRequested(creature, player, creature.getObjectId()));
		Assert.assertNull(player.getNextPacket(ErrorMessage.class));
	}
	
}
