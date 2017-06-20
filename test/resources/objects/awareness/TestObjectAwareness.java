/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.objects.awareness;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.concurrency.Delay;
import com.projectswg.common.control.Intent;
import com.projectswg.common.control.IntentManager;
import com.projectswg.common.data.location.Terrain;

import intents.PlayerEventIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.player.PlayerEvent;
import services.objects.ObjectAwareness;
import services.objects.ObjectCreator;
import test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestObjectAwareness {
	
	private GenericCreatureObject player1;
	private GenericCreatureObject player2;
	private BuildingObject building;
	private CellObject firstCell;
	private CreatureObject npc;
	
	@Before
	public void initializeTests() {
		IntentManager.setInstance(new IntentManager(1));
		IntentManager.getInstance().initialize();
		player1 = new GenericCreatureObject(1);
		player2 = new GenericCreatureObject(2);
		building = (BuildingObject) ObjectCreator.createObjectFromTemplate(10, "object/building/tatooine/shared_starport_tatooine.iff");
		firstCell = new CellObject(11);
		firstCell.setNumber(1);
		npc = (CreatureObject) ObjectCreator.createObjectFromTemplate(20, "object/mobile/dressed_tatooine_opening_wh_guard.iff");
		Assert.assertNotNull("Building is null!", building);
		Assert.assertNotNull("NPC is null!", npc);
		building.addObject(firstCell);
	}
	
	@After
	public void terminateTests() {
		IntentManager.getInstance().terminate();
	}
	
	@Test
	public void testObjectAwarenessNpcDie() {
		/*
		 * Setup:
		 *   1) Player is inside Cell #1
		 *   2) NPC is inside Cell #1
		 */
		player1.setPosition(Terrain.TATOOINE, 0, 0, 0);
		building.setPosition(Terrain.TATOOINE, 10, 10, 10);
		npc.setPosition(Terrain.TATOOINE, 0, 0, 0);
		npc.moveToContainer(firstCell);
		player1.moveToContainer(firstCell);
		
		// Create service and start sending intents
		ObjectAwareness awareness = new ObjectAwareness();
		Assert.assertTrue(awareness.initialize());
		Assert.assertTrue(awareness.start());
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(building)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(npc)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player1)));
		
		// Check to make sure NPC is in awareness
		Assert.assertTrue("Player isn't inside NPC's observer set!", npc.getObservers().contains(player1.getOwner()));
		Assert.assertTrue(fireAndWait(1000, new DestroyObjectIntent(npc)));
		Assert.assertFalse("Player is still inside NPC's observer set!", npc.getObservers().contains(player1.getOwner()));
		Assert.assertTrue(awareness.stop());
		Assert.assertTrue(awareness.terminate());
	}
	
	@Test
	public void testObjectAwarenessPlayerLogOut() {
		/*
		 * Setup:
		 *   1) Player 1 is inside Cell #1
		 *   2) Player 2 is inside Cell #1
		 *   3) NPC is inside Cell #1
		 */
		player1.setPosition(Terrain.TATOOINE, 0, 0, 0);
		player2.setPosition(Terrain.TATOOINE, 0, 0, 0);
		building.setPosition(Terrain.TATOOINE, 10, 10, 10);
		npc.setPosition(Terrain.TATOOINE, 0, 0, 0);
		npc.moveToContainer(firstCell);
		player1.moveToContainer(firstCell);
		player2.moveToContainer(firstCell);
		
		// Create service and start sending intents
		ObjectAwareness awareness = new ObjectAwareness();
		Assert.assertTrue(awareness.initialize());
		Assert.assertTrue(awareness.start());
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(building)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(npc)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player1)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player2)));
		
		// Check to make sure NPC is in awareness
		Assert.assertTrue("Player1 isn't inside NPC's observer set!", npc.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Player2 isn't inside NPC's observer set!", npc.getObservers().contains(player2.getOwner()));
		Assert.assertTrue("Player2 isn't inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertTrue("Player1 isn't inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertTrue(fireAndWait(1000, new PlayerEventIntent(player2.getOwner(), PlayerEvent.PE_DISAPPEAR)));
		Assert.assertTrue(fireAndWait(1000, new PlayerEventIntent(player2.getOwner(), PlayerEvent.PE_DESTROYED)));
		Assert.assertFalse("Player2 is still inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertFalse("Player2 is still inside NPC's observer set!", npc.getObservers().contains(player2.getOwner()));
		Assert.assertTrue("Player1 isn't inside NPC's observer set! (round 2)", npc.getObservers().contains(player1.getOwner()));
		Assert.assertTrue(fireAndWait(1000, new DestroyObjectIntent(npc)));
		Assert.assertFalse("NPC is still inside Player1's observer set!", player1.getObservers().contains(npc.getOwner()));
		Assert.assertFalse("Player2 is still inside Player1's observer set! (round 2)", player1.getObservers().contains(player2.getOwner()));
		Assert.assertTrue(awareness.stop());
		Assert.assertTrue(awareness.terminate());
	}
	
	private static boolean fireAndWait(long timeout, Intent intent) {
		intent.broadcast();
		for (int i = 0; i < timeout; i++) {
			if (intent.isComplete()) {
				Delay.sleepMilli(5);
				return true;
			}
			if (Delay.sleepMilli(1))
				return false;
		}
		return false;
	}
	
}
