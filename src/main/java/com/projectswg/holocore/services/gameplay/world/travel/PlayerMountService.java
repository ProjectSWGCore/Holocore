/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/
package com.projectswg.holocore.services.gameplay.world.travel;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureIncapacitatedIntent;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.*;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.VehicleLoader.VehicleInfo;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class PlayerMountService extends Service {
	
	// TODO no-vehicle zones (does not affect creature mounts)
	
	private final Map<CreatureObject, Set<Mount>> calledMounts;
	
	public PlayerMountService() {
		this.calledMounts = new ConcurrentHashMap<>();
	}
	
	@Override
	public boolean stop() {
		globallyStorePets();	// Don't want mounts out and about when server starts up again
		
		return true;
	}
	
	String mobileForVehicleDeed(String deedTemplate) {
		assert deedTemplate.startsWith("object/tangible/deed/") : "invalid vehicle deed";
		return deedTemplate.replace("_deed", "").replace("tangible/deed", "mobile");
	}
	
	String pcdForVehicleDeed(String deedTemplate) {
		assert deedTemplate.startsWith("object/tangible/deed/") : "invalid vehicle deed";
		return deedTemplate.replace("tangible/deed/vehicle_deed", "intangible/vehicle").replace("deed", "pcd");
	}
	
	@IntentHandler
	private void handleCreatureIncapacitatedIntent(CreatureIncapacitatedIntent cii) {
		CreatureObject player = cii.getIncappee();
		if (player.isPlayer())
			exitMount(player);
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject player = cki.getCorpse();
		if (player.isPlayer())
			exitMount(player);
	}
	
	@IntentHandler
	private void handleExecuteCommandIntent(ExecuteCommandIntent eci) {
		String cmdName = eci.getCommand().getName();
		SWGObject target = eci.getTarget();
		
		if (!(target instanceof CreatureObject))
			return;
		
		if (cmdName.equals("mount"))
			enterMount(eci.getSource(), (CreatureObject) target);
		else if (cmdName.equals("dismount"))
			exitMount(eci.getSource(), (CreatureObject) target);
	}
	
	@IntentHandler
	private void handlePlayerTransformedIntent(PlayerTransformedIntent pti) {
		CreatureObject player = pti.getPlayer();
		Set<Mount> mounts = calledMounts.get(player);
		if (mounts == null)
			return;
		
		for (Mount m : mounts) {
			if (!player.getAware().contains(m.getMount()))
				storeMount(player, m.getMount(), m.getPetControlDevice());
		}
	}
	
	@IntentHandler
	private void handleMount(MountIntent pmi) {
		enterMount(pmi.getCreature(), pmi.getPet());
	}
	
	@IntentHandler
	private void handleDismount(DismountIntent pmi) {
		exitMount(pmi.getCreature(), pmi.getPet());
	}
	
	@IntentHandler
	private void handlePetDeviceCall(PetDeviceCallIntent pci) {
		callMount(pci.getCreature(), pci.getControlDevice());
	}
	
	@IntentHandler
	private void handlePetStore(StoreMountIntent psi) {
		CreatureObject creature = psi.getCreature();
		CreatureObject mount = psi.getPet();
		
		Collection<Mount> mounts = calledMounts.get(creature);
		if (mounts != null) {
			for (Mount p : mounts) {
				if (p.getMount() == mount) {
					storeMount(creature, mount, p.getPetControlDevice());
					return;
				}
			}
		}
		SystemMessageIntent.broadcastPersonal(psi.getCreature().getOwner(), "Could not find mount to store!");
	}
	
	@IntentHandler
	private void handlePetDeviceStore(PetDeviceStoreIntent psi) {
		CreatureObject creature = psi.getCreature();
		IntangibleObject pcd = psi.getControlDevice();
		
		Collection<Mount> mounts = calledMounts.get(creature);
		if (mounts != null) {
			for (Mount mount : mounts) {
				if (mount.getPetControlDevice() == pcd) {
					storeMount(creature, mount.getMount(), pcd);
					return;
				}
			}
		}
		pcd.setCount(IntangibleObject.COUNT_PCD_STORED);
	}
	
	@IntentHandler
	private void handleVehicleDeedGenerate(VehicleDeedGenerateIntent vdgi) {
		if (!vdgi.getDeed().getTemplate().startsWith("object/tangible/deed/vehicle_deed/")) {
			SystemMessageIntent.broadcastPersonal(vdgi.getCreature().getOwner(), "Invalid vehicle deed!");
			return;
		}
		generateVehicle(vdgi.getCreature(), vdgi.getDeed());
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		if (creature == null)
			return;
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
			case PE_DISAPPEAR:
			case PE_DESTROYED:
			case PE_SERVER_KICKED:
				storeMounts(creature);
				break;
			default:
				break;
		}
	}
	
	private void generateVehicle(CreatureObject creator, SWGObject deed) {
		String pcdTemplate = pcdForVehicleDeed(deed.getTemplate());
		VehicleInfo vehicleInfo = DataLoader.vehicles().getVehicleFromPcdIff(pcdTemplate);
		if (vehicleInfo == null) {
			StandardLog.onPlayerError(this, creator, "Unknown vehicle created from deed: %s", deed.getTemplate());
			return;
		}
		IntangibleObject vehicleControlDevice = (IntangibleObject) ObjectCreator.createObjectFromTemplate(pcdTemplate);
		
		DestroyObjectIntent.broadcast(deed);
		
		vehicleControlDevice.setServerAttribute(ServerAttribute.PCD_PET_TEMPLATE, vehicleInfo.getObjectTemplate());
		vehicleControlDevice.setCount(IntangibleObject.COUNT_PCD_STORED);
		vehicleControlDevice.moveToContainer(creator.getDatapad());
		ObjectCreatedIntent.broadcast(vehicleControlDevice);
		
		callMount(creator, vehicleControlDevice);	// Once generated, the vehicle is called
	}
	
	private void callMount(CreatureObject player, IntangibleObject mountControlDevice) {
		if (player.getParent() != null || player.isInCombat()) {
			return;
		}
		if (player.getDatapad() != mountControlDevice.getParent()) {
			StandardLog.onPlayerError(this, player, "disconnecting - attempted to call another player's mount [%s]", mountControlDevice);
			CloseConnectionIntent.broadcast(player.getOwner(), DisconnectReason.SUSPECTED_HACK);
			return;
		}
		
		String template = mountControlDevice.getServerTextAttribute(ServerAttribute.PCD_PET_TEMPLATE);
		assert template != null : "mount control device doesn't have mount template attribute";
		CreatureObject mount = (CreatureObject) ObjectCreator.createObjectFromTemplate(template);
		mount.systemMove(null, player.getLocation());
		mount.addOptionFlags(OptionFlag.MOUNT);	// The mount won't appear properly if this isn't set
		mount.setPvpFaction(player.getPvpFaction());
		mount.setOwnerId(player.getObjectId());	// Client crash if this isn't set before making anyone aware
		
		// TODO after combat there's a delay
		// TODO update faction status on mount if necessary
		
		Mount mountRecord = new Mount(mountControlDevice, mount);
		Set<Mount> mounts = calledMounts.computeIfAbsent(player, c -> new CopyOnWriteArraySet<>());
		if (mountControlDevice.getCount() == IntangibleObject.COUNT_PCD_CALLED || !mounts.add(mountRecord)) {
			StandardLog.onPlayerTrace(this, player, "already called mount %s", mount);
			return;
		}
		if (mounts.size() > getMountLimit()) {
			mounts.remove(mountRecord);
			StandardLog.onPlayerTrace(this, player, "hit mount limit of %d", getMountLimit());
			return;
		}
		mountControlDevice.setCount(IntangibleObject.COUNT_PCD_CALLED);
		VehicleInfo vehicleInfo = DataLoader.vehicles().getVehicleFromIff(mount.getTemplate());
		if (vehicleInfo != null) {
			mount.setRunSpeed(vehicleInfo.getSpeed());
			mount.setWalkSpeed(vehicleInfo.getMinSpeed());
			mount.setTurnScale(vehicleInfo.getTurnRateMax());
			mount.setAccelScale(vehicleInfo.getAccelMax());
			mount.putCustomization("/private/index_speed_max", (int) (vehicleInfo.getSpeed() * 10d));
			mount.putCustomization("/private/index_speed_min", (int) (vehicleInfo.getMinSpeed() * 10d));
			mount.putCustomization("/private/index_turn_rate_min", vehicleInfo.getTurnRate());
			mount.putCustomization("/private/index_turn_rate_max", vehicleInfo.getTurnRateMax());
			mount.putCustomization("/private/index_accel_min", (int) (vehicleInfo.getAccelMin() * 10d));
			mount.putCustomization("/private/index_accel_max", (int) (vehicleInfo.getAccelMax() * 10d));
			mount.putCustomization("/private/index_decel", (int) (vehicleInfo.getDecel() * 10d));
			mount.putCustomization("/private/index_damp_roll", (int) (vehicleInfo.getDampingRoll() * 10d));
			mount.putCustomization("/private/index_damp_pitch", (int) (vehicleInfo.getDampingPitch() * 10d));
			mount.putCustomization("/private/index_damp_height", (int) (vehicleInfo.getDampingHeight() * 10d));
			mount.putCustomization("/private/index_glide", (int) (vehicleInfo.getGlide() * 10d));
			mount.putCustomization("/private/index_banking", (int) vehicleInfo.getBankingAngle());
			mount.putCustomization("/private/index_hover_height", (int) (vehicleInfo.getHoverHeight() * 10d));
			mount.putCustomization("/private/index_auto_level", (int) (vehicleInfo.getAutoLevel() * 100d));
			mount.putCustomization("/private/index_strafe", vehicleInfo.isStrafe() ? 1 : 0);
		}
		
		ObjectCreatedIntent.broadcast(mount);
		StandardLog.onPlayerTrace(this, player, "called mount %s at %s %s", mount, mount.getTerrain(), mount.getLocation().getPosition());
		cleanupCalledMounts();
	}
	
	private void enterMount(CreatureObject player, CreatureObject mount) {
		if (!isMountable(mount) || mount.getParent() != null) {
			StandardLog.onPlayerTrace(this, player, "attempted to mount %s when it's not mountable", mount);
			return;
		}
		
		if (player.getParent() != null || player.getPosture() != Posture.UPRIGHT)
			return;
		
		if (player.getObjectId() == mount.getOwnerId()) {
			player.moveToSlot(mount, "rider", mount.getArrangementId(player));
		} else if (mount.getSlottedObject("rider") != null) {
			GroupObject group = (GroupObject) ObjectLookup.getObjectById(player.getGroupId());
			if (group == null || !group.getGroupMembers().values().contains(mount.getOwnerId())) {
				StandardLog.onPlayerTrace(this, player, "attempted to mount %s when not in the same group as the owner", mount);
				return;
			}
			boolean added = false;
			for (int i = 1; i <= 7; i++) {
				if (!mount.hasSlot("rider" + i)) {
					StandardLog.onPlayerTrace(this, player, "attempted to mount %s when no slots remain", mount);
					return;
				}
				if (mount.getSlottedObject("rider" + i) == null) {
					player.moveToSlot(mount, "rider" + i, mount.getArrangementId(player));
					added = true;
					break;
				}
			}
			if (!added) {
				StandardLog.onPlayerTrace(this, player, "attempted to mount %s when no slots remain", mount);
				return;
			}
		} else {
			return;
		}
		
		player.setStatesBitmask(CreatureState.RIDING_MOUNT);
		mount.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		mount.setPosture(Posture.DRIVING_VEHICLE);
		
		VehicleInfo vehicleInfo = DataLoader.vehicles().getVehicleFromIff(mount.getTemplate());
		if (vehicleInfo != null) {
			if (!vehicleInfo.getPlayerBuff().isEmpty())
				BuffIntent.broadcast(vehicleInfo.getPlayerBuff(), player, player, false);
			if (!vehicleInfo.getVehicleBuff().isEmpty())
				BuffIntent.broadcast(vehicleInfo.getVehicleBuff(), player, mount, false);
			if (!vehicleInfo.getBuffClientEffect().isEmpty())
				player.sendObservers(new PlayClientEffectObjectMessage(vehicleInfo.getBuffClientEffect(), "", mount.getObjectId(), ""));
		}
		
		player.inheritMovement(mount);
		StandardLog.onPlayerEvent(this, player, "mounted %s", mount);
	}
	
	private void exitMount(CreatureObject player) {
		if (!player.isStatesBitmask(CreatureState.RIDING_MOUNT))
			return;
		SWGObject parent = player.getParent();
		if (!(parent instanceof CreatureObject))
			return;
		CreatureObject mount = (CreatureObject) parent;
		if (isMountable(mount) && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE))
			exitMount(player, mount);
	}
	
	private void exitMount(CreatureObject player, CreatureObject mount) {
		if (!isMountable(mount)) {
			StandardLog.onPlayerTrace(this, player, "attempted to dismount %s when it's not mountable", mount);
			return;
		}
		
		
		VehicleInfo vehicleInfo = DataLoader.vehicles().getVehicleFromIff(mount.getTemplate());
		if (player.getParent() == mount)
			dismount(player, mount, vehicleInfo);
		
		if (mount.getSlottedObject("rider") == null) {
			for (SWGObject child : mount.getSlottedObjects()) {
				assert child instanceof CreatureObject;
				dismount((CreatureObject) child, mount, vehicleInfo);
			}
			if (vehicleInfo != null && !vehicleInfo.getVehicleBuff().isEmpty())
				BuffIntent.broadcast(vehicleInfo.getVehicleBuff(), player, mount, true);
			mount.clearStatesBitmask(CreatureState.MOUNTED_CREATURE);
			mount.setPosture(Posture.UPRIGHT);
		}
	}
	
	private void storeMount(@NotNull CreatureObject player, @NotNull CreatureObject mount, @NotNull IntangibleObject mountControlDevice) {
		if (player.isInCombat())
			return;
		
		if (isMountable(mount) && mount.getOwnerId() == player.getObjectId()) {
			// Dismount anyone riding the mount about to be stored
			for (SWGObject rider : mount.getSlottedObjects()) {
				assert rider instanceof CreatureObject;
				exitMount((CreatureObject) rider, mount);
			}
			assert mount.getSlottedObjects().isEmpty();
			
			// Remove the record of the mount
			Collection<Mount> mounts = calledMounts.get(player);
			if (mounts != null)
				mounts.remove(new Mount(mountControlDevice, mount));
			
			// Destroy the mount
			mountControlDevice.setCount(IntangibleObject.COUNT_PCD_STORED);
			player.broadcast(new DestroyObjectIntent(mount));
			StandardLog.onPlayerTrace(this, player, "stored mount %s at %s %s", mount, mount.getTerrain(), mount.getLocation().getPosition());
		}
		cleanupCalledMounts();
	}
	
	private void storeMounts(CreatureObject player) {
		Collection<Mount> mounts = calledMounts.remove(player);
		if (mounts == null)
			return;
		
		for (Mount mount : mounts) {
			storeMount(player, mount.getMount(), mount.getPetControlDevice());
		}
	}
	
	/**
	 * Stores all called mounts by all players
	 */
	private void globallyStorePets() {
		calledMounts.keySet().forEach(this::storeMounts);
	}
	
	/**
	 * Removes mount records where there are no mounts called
	 */
	private void cleanupCalledMounts() {
		calledMounts.entrySet().removeIf(e -> e.getValue().isEmpty());
	}
	
	private void dismount(CreatureObject player, CreatureObject mount, VehicleInfo vehicleInfo) {
		assert player.getParent() == mount;
		player.clearStatesBitmask(CreatureState.RIDING_MOUNT);
		player.moveToContainer(null, mount.getLocation());
		player.resetMovement();
		if (vehicleInfo != null && !vehicleInfo.getPlayerBuff().isEmpty())
			BuffIntent.broadcast(vehicleInfo.getPlayerBuff(), player, mount, true);
		StandardLog.onPlayerEvent(this, player, "dismounted %s", mount);
	}
	
	private int getMountLimit() {
		return PswgDatabase.INSTANCE.getConfig().getInt(this, "mountLimit", 1);
	}
	
	private static boolean isMountable(CreatureObject mount) {
		return mount.hasOptionFlags(OptionFlag.MOUNT);
	}
	
	private static class Mount {
		
		private final IntangibleObject mountControlDevice;
		private final CreatureObject mount;
		
		private Mount(IntangibleObject mountControlDevice, CreatureObject mount) {
			this.mountControlDevice = mountControlDevice;
			this.mount = mount;
		}
		
		public IntangibleObject getPetControlDevice() {
			return mountControlDevice;
		}
		
		public CreatureObject getMount() {
			return mount;
		}
		
		@Override
		public int hashCode() {
			return mountControlDevice.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof Mount && ((Mount) o).mountControlDevice.equals(mountControlDevice);
		}
		
	}
	
}
