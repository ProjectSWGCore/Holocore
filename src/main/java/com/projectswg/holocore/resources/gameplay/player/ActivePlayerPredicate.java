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
package com.projectswg.holocore.resources.gameplay.player;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.function.Predicate;

public class ActivePlayerPredicate implements Predicate<Player> {
	@Override
	public boolean test(Player player) {
		CreatureObject creatureObject = player.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		boolean afk = playerObject.isFlagSet(PlayerFlags.AFK);
		boolean offline = playerObject.isFlagSet(PlayerFlags.LD);
		boolean incapacitated = creatureObject.getPosture() == Posture.INCAPACITATED;
		boolean dead = creatureObject.getPosture() == Posture.DEAD;
		boolean cloaked = !creatureObject.isVisible();
		boolean privateCell = false;	// Player might be inside a private building
		
		SWGObject parent = creatureObject.getParent();
		
		if (parent instanceof CellObject) {
			privateCell = !((CellObject) parent).isPublic();
		}
		
		return !afk && !offline && !incapacitated && !dead && !cloaked && !privateCell;
	}
}
