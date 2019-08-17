package com.projectswg.holocore.services.gameplay.gcw.faction;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.UpdatePvpStatusMessage;
import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.RegisterPvpZoneIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.UnregisterPvpZoneIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.StaticPvpZoneLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class FactionFlagService extends Service {
	
	private final Map<TangibleObject, Future<?>> statusChangers;
	private final ScheduledThreadPool executor;
	private final Map<String, PvpZone> pvpZones;
	
	public FactionFlagService() {
		statusChangers = new ConcurrentHashMap<>();
		executor = new ScheduledThreadPool(1, "faction-service");
		pvpZones = new ConcurrentHashMap<>();
	}
	
	@Override
	public boolean initialize() {
		Collection<StaticPvpZoneLoader.StaticPvpZoneInfo> staticPvpZones = DataLoader.Companion.staticPvpZones().getStaticPvpZones();
		
		for (StaticPvpZoneLoader.StaticPvpZoneInfo staticPvpZone : staticPvpZones) {
			int id = staticPvpZone.getId();
			Location location = staticPvpZone.getLocation();
			double radius = staticPvpZone.getRadius();
			
			PvpZone previous = pvpZones.put(String.valueOf(id), new PvpZone(location, radius));
			
			if (previous != null) {
				Log.w("Multiple static PvP zones with ID " + id);
			}
		}
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		pvpZones.clear();
		return super.terminate();
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
	
	@IntentHandler
	private void handleRegisterPvpZoneIntent(RegisterPvpZoneIntent intent) {
		String id = intent.getId();
		Location location = intent.getLocation();
		double radius = intent.getRadius();
		
		pvpZones.put(id, new PvpZone(location, radius));
	}
	
	@IntentHandler
	private void handleUnregisterPvpZoneIntent(UnregisterPvpZoneIntent intent) {
		pvpZones.remove(intent.getId());
	}
	
	@IntentHandler
	private void handlePlayerTransformedIntent(PlayerTransformedIntent intent) {
		Location newLocation = intent.getNewLocation();
		Location oldLocation = intent.getOldLocation();
		SWGObject oldParent = intent.getOldParent();
		CreatureObject creature = intent.getPlayer();
		
		if (creature.getOwner() == null) {
			return;
		}
		
		// Check if the player is attempting to enter a PvP zone
		if (!isInPvpZone(oldLocation) && isInPvpZone(newLocation)) {
			// Prevent neutrals OR rebels and imperial that are on leave from entering
			if (creature.getPvpFaction() == PvpFaction.NEUTRAL || creature.getPvpStatus() == PvpStatus.ONLEAVE) {
				// Teleport them back
				SystemMessageIntent.broadcastPersonal(creature.getOwner(), new ProsePackage("gcw", "pvp_advanced_region_not_allowed"));
				creature.moveToContainer(oldParent, oldLocation);
				return;
			}
			
			// Prevent low level players from entering PvP zones
			short level = creature.getLevel();
			
			if (level < 75) {
				// Teleport them back
				SystemMessageIntent.broadcastPersonal(creature.getOwner(), new ProsePackage("gcw", "pvp_advanced_region_level_low"));
				creature.moveToContainer(oldParent, oldLocation);
				return;
			}
			
			// Make the player Special Forces, if they are not already. Also cancels active status changes, e.g. going on leave
			handleStatusChange(creature, creature.getPvpStatus(), PvpStatus.SPECIALFORCES);
			
			SystemMessageIntent.broadcastPersonal(creature.getOwner(), new ProsePackage("gcw", "pvp_advanced_region_entered"));
		}
	}
	
	private void handleTypeChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpFaction newFaction = fi.getNewFaction();
		
		target.setPvpFaction(newFaction);
		handleFlagChange(target);
	}
	
	private void handleSwitchChange(TangibleObject target, PvpStatus oldStatus) {
		final PvpFlag pvpFlag;
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
	private void handleSwitchChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpStatus oldStatus = target.getPvpStatus();
		Player owner = target.getOwner();
		
		if (owner != null && isInPvpZone(target.getLocation())) {
			// Status changes inside a forced PvP zone are not allowed
			SystemMessageIntent.broadcastPersonal(owner, new ProsePackage("gcw", "pvp_advanced_region_cannot_go_covert"));
			return;
		}

		handleSwitchChange(target, oldStatus);
	}

	// Forces the target into the given PvpStatus
	private void handleStatusChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpStatus newStatus = fi.getNewStatus();
		PvpStatus oldStatus = target.getPvpStatus();

		handleStatusChange(target, oldStatus, newStatus);
	}

	private void handleStatusChange(TangibleObject target, PvpStatus oldStatus, PvpStatus newStatus) {
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
	
	private boolean isInPvpZone(Location location) {
		return pvpZones.values().stream()
				.anyMatch(pvpZone -> {
					Location zoneLocation = pvpZone.getLocation();
					double radius = pvpZone.getRadius();
					
					return location.isWithinFlatDistance(zoneLocation, radius);
				});
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
	
	private static class PvpZone {
		private final Location location;
		private final double radius;
		
		public PvpZone(Location location, double radius) {
			this.location = location;
			this.radius = radius;
		}
		
		public Location getLocation() {
			return location;
		}
		
		public double getRadius() {
			return radius;
		}
	}
}
