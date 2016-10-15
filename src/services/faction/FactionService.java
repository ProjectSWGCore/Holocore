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
package services.faction;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import intents.FactionIntent;
import intents.chat.ChatBroadcastIntent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.PvpStatus;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import utilities.ThreadUtilities;

public final class FactionService extends Service {

	private final ScheduledExecutorService executor;
	private final Map<TangibleObject, Future<?>> statusChangers;
	
	public FactionService() {
		statusChangers = new HashMap<>();
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("faction-service"));
		
		registerForIntent(FactionIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case FactionIntent.TYPE: handleFactionIntent((FactionIntent) i); break;
		}
	}
	
	@Override
	public boolean terminate() {
		// If some were in the middle of switching, finish the switches immediately
		executor.shutdownNow().forEach(runnable -> runnable.run());
			
		return super.terminate();
	}
	
	private void handleFactionIntent(FactionIntent i) {
		switch (i.getUpdateType()) {
			case FACTIONUPDATE:
				handleTypeChange(i);
				break;
			case SWITCHUPDATE:
				handleSwitchChange(i);
				break;
			case STATUSUPDATE:
				handleStatusChange(i);
				break;
			case FLAGUPDATE:
				handleFlagChange(i.getTarget());
				break;
		}
	}
	
	private void sendSystemMessage(TangibleObject target, String message) {
		target.getOwner().sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, message));
	}
	
	private String getBeginMessage(PvpStatus currentStatus, PvpStatus targetStatus) {
		String message = "@faction_recruiter:";
		
		if(currentStatus == PvpStatus.ONLEAVE && targetStatus == PvpStatus.COMBATANT)
			message += "on_leave_to_covert";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			message += "covert_to_overt";
		else if(currentStatus == PvpStatus.SPECIALFORCES && targetStatus == PvpStatus.COMBATANT)
			message += "overt_to_covert";
		
		return message;
	}
	
	private String getCompletionMessage(PvpStatus currentStatus, PvpStatus targetStatus) {
		String message = "@faction_recruiter:";
		
		if((currentStatus == PvpStatus.ONLEAVE || currentStatus == PvpStatus.SPECIALFORCES) && targetStatus == PvpStatus.COMBATANT)
			message += "covert_complete";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			message += "overt_complete";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.ONLEAVE )
			message += "on_leave_complete";
			
		return message;
	}
	
	private long getDelay(PvpStatus currentStatus, PvpStatus targetStatus) {
		long delay = 0;
		
		if(currentStatus == PvpStatus.ONLEAVE && targetStatus == PvpStatus.COMBATANT)
			delay = 1;
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			delay = 30;
		else if(currentStatus == PvpStatus.SPECIALFORCES && targetStatus == PvpStatus.COMBATANT)
			delay = 300;
		
		return delay;
	}
	
	private void handleTypeChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpFaction newFaction = fi.getNewFaction();
		
		target.setPvpFaction(newFaction);
		target.setPvpStatus(PvpStatus.COMBATANT);
		
		handleFlagChange(target);		// We don't want to remain attackable if we leave our current faction.
		
		if(target instanceof CreatureObject) {
			// We're given rank 1 upon joining a PvP faction
			((CreatureObject) target).setFactionRank((byte) (newFaction != PvpFaction.NEUTRAL ? 1 : 0));
		}
	}
	
	private void handleSwitchChange(FactionIntent fi) {
		final PvpFlag pvpFlag;
		final TangibleObject target = fi.getTarget();
		final PvpStatus currentStatus = target.getPvpStatus();
		final PvpStatus targetStatus;
		
		if(target.hasPvpFlag(PvpFlag.GOING_COVERT) || target.hasPvpFlag(PvpFlag.GOING_OVERT)) {
			sendSystemMessage(target, "@faction_recruiter:pvp_status_changing");
		} else {
			if(currentStatus == PvpStatus.COMBATANT) {
				pvpFlag = PvpFlag.GOING_OVERT;
				targetStatus = PvpStatus.SPECIALFORCES;
			} else {	// Covers both ONLEAVE and SPECIALFORCES
				pvpFlag = PvpFlag.GOING_COVERT;
				targetStatus = PvpStatus.COMBATANT;
			}
			
			target.setPvpFlags(pvpFlag);
			sendSystemMessage(target, getBeginMessage(currentStatus, targetStatus));
			statusChangers.put(target, executor.schedule(new Runnable() {
				@Override
				public void run() {
					statusChangers.remove(target);
					changeStatusWithMessage(target, targetStatus, getCompletionMessage(currentStatus, targetStatus));
					target.clearPvpFlags(pvpFlag);
				}
			}, getDelay(currentStatus, targetStatus), TimeUnit.SECONDS));
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
			} else if (target.getPvpStatus() == newStatus) {
				// They were in the middle of switching status when we wanted to force a new one on them
				// Let's see if their newly selected status equals the one we want to force
				
			} else {
				// Their new status does not equal the one we want - apply the new one
				target.setPvpStatus(newStatus);
			}
		} else {
			// They're not currently waiting to switch to a new status - change now
			target.setPvpStatus(newStatus);
		}
	}
	
	private void handleFlagChange(TangibleObject object) {
		Player objOwner = object.getOwner();
		
		for (SWGObject o : object.getObservers()) {
			if (!(o instanceof TangibleObject))
				continue;
			TangibleObject observer = (TangibleObject) o;
			Player obsOwner = observer.getOwner();

			int pvpBitmask = getPvpBitmask(object, observer);
			
			if (objOwner != null)
				// Send the PvP information about this observer to the owner
				objOwner.sendPacket(createPvpStatusMessage(observer, observer.getPvpFlags() | pvpBitmask));
			if (obsOwner != null)
				// Send the pvp information about the owner to this observer
				obsOwner.sendPacket(createPvpStatusMessage(object, object.getPvpFlags() | pvpBitmask));
		}
	}
	
	private void timedStatusChange() {
		
	}
	
	private void changeStatus(TangibleObject object, PvpStatus newStatus) {
		object.setPvpStatus(newStatus);
		handleFlagChange(object);
	}
	
	private void changeStatusWithMessage(TangibleObject object, PvpStatus newStatus, String systemMessage) {
		changeStatus(object, newStatus);
		new ChatBroadcastIntent(object.getOwner(), systemMessage).broadcast();
	}
	
	private UpdatePvpStatusMessage createPvpStatusMessage(TangibleObject object, int flags) {
		Set<PvpFlag> flagSet = PvpFlag.getFlags(flags);
		return new UpdatePvpStatusMessage(object.getPvpFaction(), object.getObjectId(), flagSet.toArray(new PvpFlag[flagSet.size()]));
	}
	
	private int getPvpBitmask(TangibleObject object1, TangibleObject object2) {
		int pvpBitmask = 0;

		if(object1.isEnemy(object2)) {
			pvpBitmask |= PvpFlag.AGGRESSIVE.getBitmask() | PvpFlag.ATTACKABLE.getBitmask();
		}
		
		return pvpBitmask;
	}
	
}
