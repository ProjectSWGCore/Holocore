/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 20MAX_SET_PIECES after SOE announced the official shutdown of Star Wars Galaxies. *
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
package com.projectswg.holocore.services.support.objects.items;

import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Collection;
import java.util.Objects;

public class SetBonusService extends Service {
	
	private static final int MAX_SET_PIECES = 11;
	
	public SetBonusService() {
	
	}
	
	@IntentHandler
	private void handleContainerTransferIntent(ContainerTransferIntent intent) {
		SWGObject object = intent.getObject();
		SWGObject newContainer = intent.getContainer();
		SWGObject oldContainer = intent.getOldContainer();
		
		if (newContainer == null || oldContainer == null) {
			return;
		}
		
		int minPiecesRequired = minPiecesRequired(object);
		
		if (minPiecesRequired < 0) {
			// This item doesn't have a set bonus associated with it at all
			return;
		}
		
		// TODO check if equip or unequip
		if (newContainer instanceof CreatureObject) {
			// They equipped an object
			CreatureObject creature = (CreatureObject) newContainer;
			
			int matchingPieces = matchingPieces(creature, object);
			
			if (matchingPieces < minPiecesRequired) {
				// The creature does not have enough matching pieces equipped to qualify for the first set bonus
				return;
			}
			
			int bonus = findBonus(matchingPieces - 1, object, true);
			String attribute = "@set_bonus:piece_bonus_count_" + bonus;
			String attributeValue = object.getAttribute(attribute);
			String buffName = buffName(attributeValue);
			String message = sysMessage(attributeValue);
			
			// Grant the relevant buff
			BuffIntent.broadcast(buffName, creature, creature, false);
			
			// Display relevant system message if they are a player
			Player owner = newContainer.getOwner();
			
			if (owner != null) {
				SystemMessageIntent.broadcastPersonal(owner, message);
			}
		} else if (oldContainer instanceof CreatureObject) {
			// They unequipped an object
			CreatureObject creature = (CreatureObject) oldContainer;
			
			int matchingPieces = matchingPieces(creature, object);
			boolean remove = matchingPieces < minPiecesRequired;
			
			// They still have enough pieces of the set equipped to receive a bonus
			int bonus = findBonus(matchingPieces, object, remove);	// Look for a lower rank of the set bonus to apply
			String attribute = "@set_bonus:piece_bonus_count_" + bonus;
			String attributeValue = object.getAttribute(attribute);
			String buffName = buffName(attributeValue);
			String message = sysMessage(attributeValue);
			
			BuffIntent.broadcast(buffName, creature, creature, remove);
			
			// Display relevant system message if they are a player
			Player owner = creature.getOwner();
			
			if (!remove && owner != null) {
				SystemMessageIntent.broadcastPersonal(owner, message);
			}
		}
	}
	
	private String buffName(String attributeValue) {
		return attributeValue.replace("@set_bonus:", "");
	}
	
	private String sysMessage(String attributeValue) {
		return attributeValue + "_sys";
	}
	
	private int minPiecesRequired(SWGObject object) {
		for (int i = 2; i <= 11; i++) {
			String attribute = "@set_bonus:piece_bonus_count_" + i;
			String bonus = object.getAttribute(attribute);
			
			if (bonus != null) {
				return i;
			}
		}
		
		return -1;
	}
	
	private int findBonus(int matchingPieces, SWGObject object, boolean up) {
		int start = up ? matchingPieces : MAX_SET_PIECES;
		int end = up ? MAX_SET_PIECES : matchingPieces;
		int current = start;
		
		while (current++ <= end) {
			String attribute = "@set_bonus:piece_bonus_count_" + current;
			String bonus = object.getAttribute(attribute);
			
			if (bonus != null) {
				return current;
			}
		}
		
		return matchingPieces;
	}
	
	private int matchingPieces(CreatureObject creature, SWGObject object) {
		Collection<SWGObject> slottedObjects = creature.getSlottedObjects();
		
		int counter = 0;
		
		for (SWGObject slottedObject : slottedObjects) {
			if (match(object, slottedObject)) {
				counter++;
			}
		}
		
		return counter;
	}
	
	private boolean match(SWGObject object1, SWGObject object2) {
		for (int i = 2; i <= MAX_SET_PIECES; i++) {
			String attribute = "@set_bonus:piece_bonus_count_" + i;
			String o1Bonus = object1.getAttribute(attribute);
			String o2Bonus = object2.getAttribute(attribute);
			
			if (o1Bonus == null && o2Bonus == null) {
				// Both objects don't have a set bonus for this amount of matching pieces. Move on.
				continue;
			}
			
			if (!Objects.equals(o1Bonus, o2Bonus)) {
				// Both pieces have a bonus for this amount of pieces but they do not match
				return false;
			}
		}
		
		return true;
	}
	
}
