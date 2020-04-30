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
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.*;
import java.util.stream.IntStream;

public class SetBonusService extends Service {
	
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
		
		if (newContainer instanceof CreatureObject) {
			// They equipped an object
			CreatureObject creature = (CreatureObject) newContainer;
			long equipped = matchingPieces(creature, object);
			
			if (equipped < minPiecesRequired) {
				// The creature does not have enough matching pieces equipped to qualify for the first set bonus
				return;
			}
			
			String attribute = "@set_bonus:piece_bonus_count_" + equipped;
			
			checkSetBonus(creature, object, attribute, false);
		} else if (oldContainer instanceof CreatureObject) {
			// They unequipped an object
			CreatureObject creature = (CreatureObject) oldContainer;
			
			long equipped = matchingPieces(creature, object);
			
			boolean remove = equipped < minPiecesRequired;
			long bonus = findBonus(equipped, object, remove);
			String attribute = "@set_bonus:piece_bonus_count_" + bonus;
			
			checkSetBonus(creature, object, attribute, remove);
		}
	}
	
	private void checkSetBonus(CreatureObject creature, SWGObject object, String attribute, boolean remove) {
		String attributeValue = object.getAttribute(attribute);
		
		if (attributeValue == null) {
			return;
		}
		
		String buffName = buffName(attributeValue);
		
		if (!remove && creature.hasBuff(buffName)) {
			return;
		}
		
		
		// Grant or remove the relevant buff
		BuffIntent.broadcast(buffName, creature, creature, remove);
		
		// Display relevant system message if they are a player
		if (!remove) {
			Player owner = creature.getOwner();
			
			if (owner != null) {
				String message = sysMessage(attributeValue);
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
		return bonusCounts(object)
				.reduce(Math::min)
				.orElse(-1);
	}
	
	/**
	 * Finds the nearest bonus for the given amount of matching pieces. The search can go up or down.
	 * @param matchingPieces amount of pieces equipped for this set
	 * @param object to search for the nearest bonus on
	 * @param up if {@code true} then the search will find the next available bonus. If {@code false} then the search
	 *           will find the bonus that the creature should already have.
	 * @return the number for the resulting bonus
	 */
	private long findBonus(long matchingPieces, SWGObject object, boolean up) {
		return up ? findNextBonus(matchingPieces, object) : findPrevBonus(matchingPieces, object);
	}
	
	private long findNextBonus(long matchingPieces, SWGObject object) {
		return bonusCounts(object)
				.filter(count -> count >= matchingPieces)
				.reduce(Math::min)
				.orElse(0);
	}
	
	private long findPrevBonus(long matchingPieces, SWGObject object) {
		return bonusCounts(object)
				.filter(count -> count <= matchingPieces)
				.reduce(Math::max)
				.orElse(0);
	}
	
	private long matchingPieces(CreatureObject creature, SWGObject object) {
		return creature.getSlottedObjects().stream()
				.distinct()	// An item can occupy multiple slots - ensure an item is only counted once
				.map(slottedObject -> slottedObject.getServerAttribute(ServerAttribute.SET_BONUS_ID))
				.filter(Objects::nonNull)
				.filter(setId -> Objects.equals(setId, object.getServerAttribute(ServerAttribute.SET_BONUS_ID)))
				.count();
	}
	
	private IntStream bonusCounts(SWGObject object) {
		return object.getAttributes().keySet().stream()
				.filter(attrName -> attrName.startsWith("@set_bonus:piece_bonus_count_"))
				.map(attrName -> attrName.replace("@set_bonus:piece_bonus_count_", ""))
				.mapToInt(Integer::parseInt);
	}
	
}
