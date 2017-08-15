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
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import intents.PlayerEventIntent;
import intents.object.ContainerTransferIntent;
import intents.object.DestroyObjectIntent;
import intents.object.MoveObjectIntent;
import intents.object.ObjectCreatedIntent;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.creature.CreatureState;
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
	
	@Test
	public void testVehicleMount() {
		CreatureObject vehicle = (CreatureObject) ObjectCreator.createObjectFromTemplate("object/mobile/vehicle/shared_barc_speeder.iff");
		player1.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		player2.setPosition(Terrain.TATOOINE, 3510, 5, -4810); // 12ish meters away
		vehicle.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		
		ObjectAwareness awareness = new ObjectAwareness();
		Assert.assertTrue(awareness.initialize());
		Assert.assertTrue(awareness.start());
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player1)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player2)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(vehicle)));
		
		Assert.assertTrue("Player1 is not inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Player2 is not inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertTrue("Vehicle is not inside Player1's awareness set!", player1.getObjectsAware().contains(vehicle));
		Assert.assertTrue("Vehicle is not inside Player2's awareness set!", player2.getObjectsAware().contains(vehicle));
		Assert.assertNull("Player1 is in a parent!", player1.getParent());
		
		// Mount
		Assert.assertTrue(fireAndWait(100, new ContainerTransferIntent(player1, null,vehicle))); 
		player1.setStatesBitmask(CreatureState.RIDING_MOUNT);
		vehicle.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.DRIVING_VEHICLE);
		
		Assert.assertTrue("Player1 is not inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Player2 is not inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertFalse("Vehicle is still inside Player1's awareness set!", player1.getObjectsAware().contains(vehicle));
		Assert.assertTrue("Vehicle is not inside Player2's awareness set!", player2.getObjectsAware().contains(vehicle));
		Assert.assertNotNull("Player1 is not in a parent!", player1.getParent());
		Assert.assertEquals("Player1 is not mounted to the vehicle!", vehicle, player1.getParent());
		
		Assert.assertTrue(fireAndWait(100, new MoveObjectIntent(player1, new Location(3510, 5, -4810, Terrain.TATOOINE), 7.3, 1)));
		
		Assert.assertTrue("Player1 is not inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Player2 is not inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertFalse("Vehicle is inside Player1's awareness set!", player1.getObjectsAware().contains(vehicle));
		Assert.assertTrue("Vehicle is not inside Player2's awareness set!", player2.getObjectsAware().contains(vehicle));
		Assert.assertNotNull("Player1 is not in a parent!", player1.getParent());
		Assert.assertEquals("Player1 is not mounted to the vehicle!", vehicle, player1.getParent());
		
		Assert.assertTrue(awareness.stop());
		Assert.assertTrue(awareness.terminate());
	}
	
	@Test
	public void testVehicleMove() {
		CreatureObject vehicle = (CreatureObject) ObjectCreator.createObjectFromTemplate("object/mobile/vehicle/shared_barc_speeder.iff");
		player1.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		player2.setPosition(Terrain.TATOOINE, 3510, 5, -4810); // 12ish meters away
		vehicle.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		npc.setPosition(Terrain.TATOOINE, 3000, 5, 4000);
		
		ObjectAwareness awareness = new ObjectAwareness();
		Assert.assertTrue(awareness.initialize());
		Assert.assertTrue(awareness.start());
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player1)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(player2)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(npc)));
		Assert.assertTrue(fireAndWait(100, new ObjectCreatedIntent(vehicle)));
		Assert.assertTrue(fireAndWait(100, new ContainerTransferIntent(player1, null, vehicle))); 
		player1.setStatesBitmask(CreatureState.RIDING_MOUNT);
		vehicle.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.DRIVING_VEHICLE);
		
		Assert.assertTrue("Player1 is not inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Player2 is not inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertFalse("Vehicle is still inside Player1's awareness set!", player1.getObjectsAware().contains(vehicle));
		Assert.assertTrue("Vehicle is not inside Player2's awareness set!", player2.getObjectsAware().contains(vehicle));
		Assert.assertNotNull("Player1 is not in a parent!", player1.getParent());
		Assert.assertEquals("Player1 is not mounted to the vehicle!", vehicle, player1.getParent());
		
		Assert.assertTrue(fireAndWait(100, new MoveObjectIntent(player1, new Location(3000, 5, 4010, Terrain.TATOOINE), 7.3, 1)));
		
		Assert.assertFalse("Player1 is still inside Player2's observer set!", player2.getObservers().contains(player1.getOwner()));
		Assert.assertFalse("Player2 is still inside Player1's observer set!", player1.getObservers().contains(player2.getOwner()));
		Assert.assertFalse("Vehicle is inside Player1's awareness set!", player1.getObjectsAware().contains(vehicle));
		Assert.assertFalse("Vehicle is still inside Player2's awareness set!", player2.getObjectsAware().contains(vehicle));
		Assert.assertNotNull("Player1 is not in a parent!", player1.getParent());
		Assert.assertEquals("Player1 is not mounted to the vehicle!", vehicle, player1.getParent());
		
		Assert.assertTrue("Player1 is not inside NPC's observer set!", npc.getObservers().contains(player1.getOwner()));
		Assert.assertTrue("Vehicle is not inside NPC's awareness set!", npc.getObjectsAware().contains(vehicle));
		
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
