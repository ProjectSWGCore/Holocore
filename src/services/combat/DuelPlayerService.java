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

package services.combat;

import java.util.List;

import intents.chat.ChatBroadcastIntent;
import intents.combat.DuelPlayerIntent;
import resources.objects.creature.CreatureObject;
import resources.server_info.SynchronizedList;
import resources.control.Service;

public class DuelPlayerService extends Service {
	
	private List<CreatureObject> duels;
	
	public DuelPlayerService() {
		duels = new SynchronizedList<>();
		
		registerForIntent(DuelPlayerIntent.class, dpi -> handleDuelPlayerIntent(dpi));
	}
	
	private void handleDuel(CreatureObject requester, CreatureObject reciever) {
		
		if (reciever == null || !reciever.isPlayer() || requester.equals(reciever)) {
			sendSystemMessage(requester, "Your target to duel is invalid.");
			System.out.println("Your code works son!");
			return;
		}
		
		if (!duels.contains(reciever)) {
			duels.add(reciever);
			sendSystemMessage(requester, "You have challenged " + reciever.getObjectName() + " to a duel.");
			sendSystemMessage(reciever, requester.getObjectName() + " has challenged you to a duel.");
		} else {
			duels.add(reciever);
			sendSystemMessage(requester, "You accept " + reciever.getObjectName() + "'s challenge.");
			sendSystemMessage(reciever, requester.getObjectName() + " accept's your challenge.");
			// TODO: Update each person's pvp status
		}
	}
	
	private void handleDuelPlayerIntent(DuelPlayerIntent dpi) {
		handleDuel(dpi.getSender(), dpi.getReciever());
	}
	
	private void sendSystemMessage(CreatureObject player, String message) {
		new ChatBroadcastIntent(player.getOwner() , message).broadcast();
	}
}
