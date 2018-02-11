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
package com.projectswg.holocore.services.faction;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.control.Service;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.network.packets.swg.zone.UpdatePvpStatusMessage;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import com.projectswg.holocore.intents.FactionIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.player.Player;

public final class FactionService extends Service {

	private final Map<TangibleObject, Future<?>> statusChangers;
	private final PswgScheduledThreadPool executor;
	
	public FactionService() {
		statusChangers = new ConcurrentHashMap<>();
		executor = new PswgScheduledThreadPool(1, "faction-service");
		
		registerForIntent(FactionIntent.class, this::handleFactionIntent);
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		executor.awaitTermination(500);
		
		return super.terminate();
	}
	
	private void handleFactionIntent(FactionIntent fi) {
		switch (fi.getUpdateType()) {
			case FACTIONUPDATE:
				handleTypeChange(fi);
				break;
			case SWITCHUPDATE:
				handleSwitchChange(fi);
				break;
			case STATUSUPDATE:
				handleStatusChange(fi);
				break;
			case FLAGUPDATE:
				handleFlagChange(fi.getTarget());
				break;
		}
	}
	
	private void handleTypeChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpFaction newFaction = fi.getNewFaction();
		
		target.setPvpFaction(newFaction);
		handleFlagChange(target);
		
		if(target.getBaselineType() == BaselineType.CREO && target.getPvpFaction() != PvpFaction.NEUTRAL) {
			// We're given rank 1 upon joining a non-neutral faction
			((CreatureObject) target).setFactionRank((byte) 1);
		}
	}
	
	private void handleSwitchChange(FactionIntent fi) {
		final PvpFlag pvpFlag;
		final TangibleObject target = fi.getTarget();
		final PvpStatus oldStatus = target.getPvpStatus();
		final PvpStatus newStatus;
		
		if(target.hasPvpFlag(PvpFlag.GOING_COVERT) || target.hasPvpFlag(PvpFlag.GOING_OVERT)) {
			SystemMessageIntent.broadcastPersonal(target.getOwner(), "@faction_recruiter:pvp_status_changing");
		} else {
			if(oldStatus == PvpStatus.COMBATANT) {
				pvpFlag = PvpFlag.GOING_OVERT;
				newStatus = PvpStatus.SPECIALFORCES;
			} else {	// Covers both ONLEAVE and SPECIALFORCES
				pvpFlag = PvpFlag.GOING_COVERT;
				newStatus = PvpStatus.COMBATANT;
			}
			
			target.setPvpFlags(pvpFlag);
			SystemMessageIntent.broadcastPersonal(target.getOwner(), getBeginMessage(oldStatus, newStatus));
			statusChangers.put(target, executor.execute(getDelay(oldStatus, newStatus) * 1000, () -> completeChange(target, pvpFlag, oldStatus, newStatus)));
		}
	}
	
	// Forces the target into the given PvpStatus
	private void handleStatusChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpStatus oldStatus = target.getPvpStatus();
		PvpStatus newStatus = fi.getNewStatus();
		
		// No reason to send deltas and all that if the status isn't effectively changing
		if(oldStatus == newStatus)
			return;
		
		// Let's clear PvP flags in case they were in the middle of going covert/overt
		Future<?> future = statusChangers.remove(target);

		if (future != null) {
			if (future.cancel(false)) {
				target.clearPvpFlags(PvpFlag.GOING_COVERT, PvpFlag.GOING_OVERT);
			} else if (target.getPvpStatus() != newStatus) {
				// Their new status does not equal the one we want - apply the new one
				changeStatus(target, newStatus);
			}
		} else {
			// They're not currently waiting to switch to a new status - change now
			changeStatus(target, newStatus);
		}
	}
	
	private void handleFlagChange(TangibleObject target) {
		Player objOwner = target.getOwner();
		
		for (Player observerOwner : target.getObservers()) {
			TangibleObject observer = observerOwner.getCreatureObject();

			int pvpBitmask = getPvpBitmask(target, observer);
			
			if (objOwner != null) // Send the PvP information about this observer to the owner
				objOwner.sendPacket(createPvpStatusMessage(observer, observer.getPvpFlags() | pvpBitmask));
			// Send the pvp information about the owner to this observer
			observerOwner.sendPacket(createPvpStatusMessage(target, target.getPvpFlags() | pvpBitmask));
		}
	}
	
	private void completeChange(TangibleObject target, PvpFlag pvpFlag, PvpStatus oldStatus, PvpStatus newStatus) {
		statusChangers.remove(target);
		
		SystemMessageIntent.broadcastPersonal(target.getOwner(), getCompletionMessage(oldStatus, newStatus));
		target.clearPvpFlags(pvpFlag);
		changeStatus(target, newStatus);
	}
	
	private void changeStatus(TangibleObject target, PvpStatus newStatus) {
		target.setPvpStatus(newStatus);
		handleFlagChange(target);
	}
	
	private static String getBeginMessage(PvpStatus oldStatus, PvpStatus newStatus) {
		String message = "@faction_recruiter:";
		
		if(oldStatus == PvpStatus.ONLEAVE && newStatus == PvpStatus.COMBATANT)
			message += "on_leave_to_covert";
		else if(oldStatus == PvpStatus.COMBATANT && newStatus == PvpStatus.SPECIALFORCES)
			message += "covert_to_overt";
		else if(oldStatus == PvpStatus.SPECIALFORCES && newStatus == PvpStatus.COMBATANT)
			message += "overt_to_covert";
		
		return message;
	}
	
	private static String getCompletionMessage(PvpStatus oldStatus, PvpStatus newStatus) {
		String message = "@faction_recruiter:";
		
		if((oldStatus == PvpStatus.ONLEAVE || oldStatus == PvpStatus.SPECIALFORCES) && newStatus == PvpStatus.COMBATANT)
			message += "covert_complete";
		else if(oldStatus == PvpStatus.COMBATANT && newStatus == PvpStatus.SPECIALFORCES)
			message += "overt_complete";
		else if(oldStatus == PvpStatus.COMBATANT && newStatus == PvpStatus.ONLEAVE )
			message += "on_leave_complete";
			
		return message;
	}
	
	private static long getDelay(PvpStatus oldStatus, PvpStatus newStatus) {
		long delay = 0;
		
		if(oldStatus == PvpStatus.ONLEAVE && newStatus == PvpStatus.COMBATANT)
			delay = 1;
		else if(oldStatus == PvpStatus.COMBATANT && newStatus == PvpStatus.SPECIALFORCES)
			delay = 30;
		else if(oldStatus == PvpStatus.SPECIALFORCES && newStatus == PvpStatus.COMBATANT)
			delay = 300;
		
		return delay;
	}
	
	private static UpdatePvpStatusMessage createPvpStatusMessage(TangibleObject target, int flags) {
		Set<PvpFlag> flagSet = PvpFlag.getFlags(flags);
		return new UpdatePvpStatusMessage(target.getPvpFaction(), target.getObjectId(), flagSet.toArray(new PvpFlag[flagSet.size()]));
	}
	
	private static int getPvpBitmask(TangibleObject target, TangibleObject observer) {
		int pvpBitmask = 0;

		if(target.isEnemy(observer)) {
			pvpBitmask |= PvpFlag.AGGRESSIVE.getBitmask() | PvpFlag.ATTACKABLE.getBitmask() | PvpFlag.ENEMY.getBitmask();
		}
		
		return pvpBitmask;
	}
	
}
