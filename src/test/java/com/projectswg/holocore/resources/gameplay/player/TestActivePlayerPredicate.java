/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.gameplay.player;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestActivePlayerPredicate {
	
	private final ActivePlayerPredicate predicate;
	
	public TestActivePlayerPredicate() {
		predicate = new ActivePlayerPredicate();
	}
	
	private Player player;
	private CreatureObject creatureObject;
	private PlayerObject playerObject;
	
	@BeforeEach
	public void setup() {
		player = new GenericPlayer();
		creatureObject = new GenericCreatureObject(1);
		playerObject = creatureObject.getPlayerObject();
		player.setCreatureObject(creatureObject);
	}
	
	@Test
	public void testAfk() {
		playerObject.getFlags().set(PlayerFlags.AFK);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testOffline() {
		playerObject.getFlags().set(PlayerFlags.LD);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testIncapacitated() {
		creatureObject.setPosture(Posture.INCAPACITATED);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testDead() {
		creatureObject.setPosture(Posture.DEAD);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testCloaked() {
		creatureObject.setVisible(false);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testInsidePrivateCell() {
		CellObject cell = new CellObject(2);
		cell.setPublic(false);
		creatureObject.systemMove(cell);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testActive() {
		boolean actual = predicate.test(player);
		
		assertTrue(actual);
	}
}
