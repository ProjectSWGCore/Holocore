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
	
	private final List<CreatureObject> duels;
	
	public DuelPlayerService() {
		duels = new SynchronizedList<>();
		
		registerForIntent(DuelPlayerIntent.class, dpi -> handleDuelPlayerIntent(dpi));
	}
	
	private void handleAcceptDuel(CreatureObject accepter, CreatureObject target) {
		duels.add(target);
		sendSystemMessage(accepter, "You accept " + target.getObjectName() + "'s challenge.");
		sendSystemMessage(target, accepter.getObjectName() + " accepts your challenge.");
		// TODO: Update each person's pvp status
	}
	
	private void handleEndDuel(CreatureObject ender, CreatureObject target) {
		if (duels.contains(target)) {
			sendSystemMessage(ender, "You end your duel with " + target.getObjectName() + ".");
			sendSystemMessage(target, ender.getObjectName() + " ends your duel.");
			duels.remove(ender);
			duels.remove(target);
			// TODO: Update each person's pvp status
		} else {
			sendSystemMessage(ender, "You are not dueling " + target.getObjectName() + ".");
		}
	}
	
	private void handleCancelDuel(CreatureObject canceler, CreatureObject target) {
		// TODO: Implement
	}
	
	private void handleDeclineDuel(CreatureObject decliner, CreatureObject target) {
		// TODO: Implement
	}
	
	private void handleRequestDuel(CreatureObject requester, CreatureObject target) {
		if (!duels.contains(requester)) {
			duels.add(target);
			sendSystemMessage(requester, "You challenge " + target.getObjectName() + " to a duel.");
			sendSystemMessage(target, requester.getObjectName() + " challenges you to a duel.");
		} else {
			sendSystemMessage(requester, "You already challenged " + target.getObjectName() + " to a duel");
		}
	}
	
	private void checkForEventTypeCorrection(DuelPlayerIntent dpi) {
		if (dpi.getEventType() == DuelPlayerIntent.DuelEventType.REQUEST && duels.contains(dpi.getSender())) {
			dpi.setDuelEventType(DuelPlayerIntent.DuelEventType.ACCEPT);
		}
	}
	
	private void handleDuelPlayerIntent(DuelPlayerIntent dpi) {
		if (dpi.getReciever() == null || !dpi.getReciever().isPlayer() || dpi.getSender().equals(dpi.getReciever())) {
			return;
		}
		
		checkForEventTypeCorrection(dpi);
		
		switch (dpi.getEventType()) {
			case ACCEPT:
				handleAcceptDuel(dpi.getSender(), dpi.getReciever());
				break;
			case CANCEL:
				handleCancelDuel(dpi.getSender(), dpi.getReciever());
				break;
			case DECLINE:
				handleDeclineDuel(dpi.getSender(), dpi.getReciever());
				break;
			case END:
				handleEndDuel(dpi.getSender(), dpi.getReciever());
				break;
			case REQUEST:
				handleRequestDuel(dpi.getSender(), dpi.getReciever());
				break;
		}
	}
	
	public List<CreatureObject> getDuelsList() {
		return duels;
	}
	
	private void sendSystemMessage(CreatureObject player, String message) {
		new ChatBroadcastIntent(player.getOwner() , message).broadcast();
	}
}
