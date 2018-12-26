package com.projectswg.holocore.services.gameplay.gcw.faction;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.network.packets.swg.zone.UpdatePvpStatusMessage;
import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class FactionFlagService extends Service {
	
	private final Map<TangibleObject, Future<?>> statusChangers;
	private final ScheduledThreadPool executor;
	
	public FactionFlagService() {
		statusChangers = new ConcurrentHashMap<>();
		executor = new ScheduledThreadPool(1, "faction-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	@IntentHandler
	private void handleFactionIntent(FactionIntent fi) {
		TangibleObject target  = fi.getTarget();
		
		if (!(target instanceof CreatureObject) && !(target.getParent() instanceof CellObject)) // We don't deal with faction updates for inventory items and the like
			return;
		
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
				handleFlagChange(target);
				break;
		}
	}
	
	@IntentHandler
	private void handleDuelPlayerIntent(DuelPlayerIntent dpi) {
		if (dpi.getReciever() == null || !dpi.getReciever().isPlayer() || dpi.getSender().equals(dpi.getReciever())) {
			return;
		}
		
		switch (dpi.getEventType()) {
			case BEGINDUEL:
				handleBeginDuel(dpi.getSender(), dpi.getReciever());
				break;
			case END:
				handleEndDuel(dpi.getSender(), dpi.getReciever());
				break;
			default:
				break;
		}
	}
		
	
	private void handleTypeChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpFaction newFaction = fi.getNewFaction();
		
		target.setPvpFaction(newFaction);
		handleFlagChange(target);
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
		Player targetOwner = target.getOwner();
		
		for (SWGObject objectAware : target.getObjectsAware()) {
			if (!(objectAware instanceof TangibleObject)) {
				continue;
			}
			
			TangibleObject tangibleAware = (TangibleObject) objectAware;
			
			Player observerOwner = tangibleAware.getOwner();
			
			if (targetOwner != null) // Send the PvP information about this observer to the owner
				targetOwner.sendPacket(createPvpStatusMessage(target, tangibleAware));
			
			if (observerOwner != null)	// Send the pvp information about the owner to this observer
				observerOwner.sendPacket(createPvpStatusMessage(tangibleAware, target));
		}
	}
	
	private void handleBeginDuel(CreatureObject accepter, CreatureObject target) {
		accepter.sendSelf(createPvpStatusMessage(accepter, target));
		target.sendSelf(createPvpStatusMessage(target, accepter));
	}
	
	private void handleEndDuel(CreatureObject accepter, CreatureObject target) {
		accepter.sendSelf(createPvpStatusMessage(accepter, target));
		target.sendSelf(createPvpStatusMessage(target, accepter));
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
	
	private static UpdatePvpStatusMessage createPvpStatusMessage(TangibleObject self, TangibleObject target) {
		return new UpdatePvpStatusMessage(target.getPvpFaction(), target.getObjectId(), self.getPvpFlagsFor(target));
	}
	
}
