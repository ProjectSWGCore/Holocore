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
package com.projectswg.holocore.services.gameplay.combat.duel;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.DeathblowIntent;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class DuelService extends Service {
	
	public DuelService() {
		
	}
	
	@IntentHandler
	private void handleDuelPlayerIntent(DuelPlayerIntent dpi) {
		if (dpi.getReciever() == null || !dpi.getReciever().isPlayer() || dpi.getSender().equals(dpi.getReciever())) {
			return;
		}
		
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
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleDeathblowIntent(DeathblowIntent di) {
		CreatureObject killer = di.getKiller();
		CreatureObject corpse = di.getCorpse();
		
		if (!corpse.isPlayer()) {
			return;
		}
		
		if (corpse.isDuelingPlayer(killer)) {
			handleEndDuel(corpse, killer);
		}
		
	}	
	
	@IntentHandler
	private void handlCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject killer = cki.getKiller();
		CreatureObject corpse = cki.getCorpse();
		
		if (!corpse.isPlayer()) {
			return;
		}
		
		if (corpse.isDuelingPlayer(killer)) {
			handleEndDuel(corpse, killer);
		}
		
	}	
	
	private void handleAcceptDuel(CreatureObject accepter, CreatureObject target) {
		if (!isLocationValidToDuel(accepter, target)) {
			sendSystemMessage(accepter, target, "not_available_to_duel");
			accepter.removePlayerFromSentDuels(target);
			target.removePlayerFromSentDuels(accepter);
			return;
		}
		if (accepter.isDuelingPlayer(target)) {
			sendSystemMessage(accepter, target, "already_dueling");
			return;
		}
		
		accepter.addPlayerToSentDuels(target);
		sendSystemMessage(accepter, target, "accept_self");
		sendSystemMessage(target, accepter, "accept_target");
		accepter.setPvpFlags(PvpFlag.DUEL);
		target.setPvpFlags(PvpFlag.DUEL);
		
		EnterCombatIntent.broadcast(accepter, target);
		EnterCombatIntent.broadcast(target, accepter);

		new DuelPlayerIntent(accepter, target, DuelPlayerIntent.DuelEventType.BEGINDUEL).broadcast();

	}
	
	private void endDuel(CreatureObject ender, CreatureObject target) {
		ender.clearPvpFlags(PvpFlag.DUEL);
		target.clearPvpFlags(PvpFlag.DUEL);
		ender.removePlayerFromSentDuels(target);
		target.removePlayerFromSentDuels(ender);
		
		ExitCombatIntent.broadcast(ender);
		ExitCombatIntent.broadcast(target);

	}
	
	private void handleEndDuel(CreatureObject ender, CreatureObject target) {
		if (ender.isDuelingPlayer(target)) {
			sendSystemMessage(ender, target, "end_self");
			sendSystemMessage(target, ender, "end_target");
			endDuel(ender, target);
		} else {
			sendSystemMessage(ender, target, "not_dueling");
		}
	}
	
	private void handleCancelDuel(CreatureObject canceler, CreatureObject target) {
		canceler.removePlayerFromSentDuels(target);
		sendSystemMessage(canceler, target, "cancel_self");
		sendSystemMessage(target, canceler, "cancel_target");
	}
	
	private void handleDeclineDuel(CreatureObject decliner, CreatureObject target) {
		target.removePlayerFromSentDuels(decliner);
		sendSystemMessage(decliner, target, "reject_self");
		sendSystemMessage(target, decliner, "reject_target");
	}
	
	private void handleRequestDuel(CreatureObject requester, CreatureObject target) {
		if (!isLocationValidToDuel(requester, target)) {
			sendSystemMessage(requester, target, "not_available_to_duel");
			return;
		}
		if (!requester.hasSentDuelRequestToPlayer(target)) {
			requester.addPlayerToSentDuels(target);
			sendSystemMessage(requester, target, "challenge_self");
			sendSystemMessage(target, requester, "challenge_target");
		} else if (requester.isDuelingPlayer(target)) {
			sendSystemMessage(requester, target, "already_dueling");
		} else {
			sendSystemMessage(requester, target, "already_challenged");
		}
	}
	
	private boolean isLocationValidToDuel(CreatureObject creature1, CreatureObject creature2) {
		Location location1 = creature1.getWorldLocation();
		Location location2 = creature2.getWorldLocation();
		if (location1.getTerrain() != location2.getTerrain())
			return false;
		return location1.isWithinDistance(location2, 32);
	}
	
	private void sendSystemMessage(CreatureObject playerToMessage, CreatureObject playerToMessageAbout, String message) {
		new SystemMessageIntent(playerToMessage.getOwner() , new ProsePackage(new StringId("duel", message), "TT", playerToMessageAbout.getObjectName())).broadcast();
	}
}
