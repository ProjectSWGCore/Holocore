/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/

package com.projectswg.holocore.services.support.objects.radials;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuRequest;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuResponse;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestRadialService extends TestRunnerSimulatedWorld {
	
	@BeforeEach
	public void initServices() {
		registerService(new RadialService());
	}
	
	@Test
	public void testInvalidTarget() {
		GenericCreatureObject creature = new GenericCreatureObject(getUniqueId());
		TangibleObject tangible = new GenericTangibleObject(getUniqueId());
		registerObject(creature);
		
		sendRequest(creature, tangible, RadialItem.ITEM_USE, RadialItem.ITEM_DESTROY);
		
		ObjectMenuResponse response = creature.getOwner().getNextPacket(ObjectMenuResponse.class);
		assertNull(response);
	}
	
	@Test
	public void testDefaultRadials() {
		GenericCreatureObject creature = new GenericCreatureObject(getUniqueId());
		TangibleObject tangible = new GenericTangibleObject(getUniqueId());
		registerObject(creature, tangible);
		
		sendRequest(creature, tangible, RadialItem.ITEM_USE, RadialItem.ITEM_DESTROY);
		
		ObjectMenuResponse response = creature.getOwner().getNextPacket(ObjectMenuResponse.class);
		assertNotNull(response);
		assertEquals(creature.getObjectId(), response.getRequestorId());
		assertEquals(tangible.getObjectId(), response.getTargetId());
		assertEquals(2, response.getOptions().size());
		assertEquals(RadialItem.ITEM_USE, response.getOptions().get(0).getType());
		assertEquals(RadialItem.ITEM_DESTROY, response.getOptions().get(1).getType());
	}
	
	@Test
	public void testReplaceLootRadial() {
		GenericCreatureObject creature = new GenericCreatureObject(getUniqueId());
		AIObject dead = new AIObject(getUniqueId());
		registerObject(creature, dead);
		dead.setPosture(Posture.DEAD);
		
		sendRequest(creature, dead, RadialItem.LOOT_ALL, RadialItem.EXAMINE);
		
		ObjectMenuResponse response = creature.getOwner().getNextPacket(ObjectMenuResponse.class);
		assertNotNull(response);
		assertEquals(creature.getObjectId(), response.getRequestorId());
		assertEquals(dead.getObjectId(), response.getTargetId());
		
		assertEquals(2, response.getOptions().size());
		assertEquals(RadialItem.LOOT_ALL, response.getOptions().get(0).getType());
		assertEquals(RadialItem.EXAMINE, response.getOptions().get(1).getType());
		
		assertEquals(1, response.getOptions().get(0).getChildren().size());
		assertEquals(RadialItem.LOOT, response.getOptions().get(0).getChildren().get(0).getType());
	}
	
	private void sendRequest(CreatureObject source, SWGObject target, RadialItem ... items) {
		ObjectMenuRequest request = new ObjectMenuRequest(source.getObjectId());
		request.setTargetId(target.getObjectId());
		request.setRequestorId(source.getObjectId());
		for (RadialItem item : items)
			request.addOption(RadialOption.create(item));
		request.setCounter((byte) 1);
		broadcastAndWait(new InboundPacketIntent(source.getOwner(), request));
	}
	
}
