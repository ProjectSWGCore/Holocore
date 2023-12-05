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
package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.resources.support.random.RandomDie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCombatCommandAttack {
	@Test
	public void testConeRange() {
		Location attackerLocation = new Location.LocationBuilder()
				.setX(0)
				.setY(0)
				.setZ(0)
				.build();
		
		Location targetLocation = new Location.LocationBuilder()
				.setX(20)
				.setY(0)
				.setZ(10)
				.build();
		
		Location collateralInsideCone1 = new Location.LocationBuilder()
				.setX(10)
				.setY(0)
				.setZ(5)
				.build();
		
		Location collateralInsideCone2 = new Location.LocationBuilder()
				.setX(25)
				.setY(0)
				.setZ(10)
				.build();
		
		Location collateralOutsideCone = new Location.LocationBuilder()
				.setX(-20)
				.setY(0)
				.setZ(-15)
				.build();
		
		double dirX = targetLocation.getX() - attackerLocation.getX();
		double dirZ = targetLocation.getZ() - attackerLocation.getZ();
		
		CombatCommandAttack instance = new CombatCommandAttack(new RandomDie(), new RandomDie());
		assertTrue(instance.isInConeAngle(attackerLocation, collateralInsideCone1, 30, dirX, dirZ));
		assertTrue(instance.isInConeAngle(attackerLocation, collateralInsideCone2, 30, dirX, dirZ));
		assertFalse(instance.isInConeAngle(attackerLocation, collateralOutsideCone, 30, dirX, dirZ));
	}
}
