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
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.*;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Mads on 19-06-2017.
 */
public class PlayerMountService extends Service {
	
	// TODO no-vehicle zones (does not affect creature mounts)
	
	private final Map<CreatureObject, Set<Mount>> calledMounts;	// TODO rename Mount class and this field
	
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
	private void handleExecuteCommandIntent(ExecuteCommandIntent eci) {
		String cmdName = eci.getCommand().getName();
		SWGObject target = eci.getTarget();
		
		Log.t("Execute: '%s' with target: '%s' and args: '%s'", eci.getCommand().getName(), eci.getTarget(), eci.getArguments());
		if (!(target instanceof CreatureObject))
			return;
		
		if (cmdName.equals("mount"))
			enterMount(eci.getSource().getOwner(), (CreatureObject) target);
		else if (cmdName.equals("dismount"))
			exitMount(eci.getSource().getOwner(), (CreatureObject) target);
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
		enterMount(pmi.getPlayer(), pmi.getPet());
	}
	
	@IntentHandler
	private void handleDismount(DismountIntent pmi) {
		exitMount(pmi.getPlayer(), pmi.getPet());
	}
	
	@IntentHandler
	private void handlePetDeviceCall(PetDeviceCallIntent pci) {
		callMount(pci.getPlayer().getCreatureObject(), pci.getControlDevice());
	}
	
	@IntentHandler
	private void handlePetStore(StoreMountIntent psi) {
		CreatureObject creature = psi.getPlayer().getCreatureObject();
		CreatureObject mount = psi.getPet();
		
		Collection<Mount> mounts = calledMounts.get(creature);
		if (mounts == null)
			return;
		
		for (Mount p : mounts) {
			if (p.getMount() == mount) {
				storeMount(creature, mount, p.getPetControlDevice());
				return;
			}
		}
	}
	
	@IntentHandler
	private void handlePetDeviceStore(PetDeviceStoreIntent psi) {
		CreatureObject creature = psi.getPlayer().getCreatureObject();
		IntangibleObject pcd = psi.getControlDevice();
		
		Collection<Mount> mounts = calledMounts.get(creature);
		if (mounts == null)
			return;
		
		for (Mount mount : mounts) {
			if (mount.getPetControlDevice() == pcd) {
				storeMount(creature, mount.getMount(), pcd);
				return;
			}
		}
	}
	
	@IntentHandler
	private void handleVehicleDeedGenerate(VehicleDeedGenerateIntent vdgi) {
		if (!vdgi.getDeed().getTemplate().startsWith("object/tangible/deed/vehicle_deed/"))
			return;
		generateVehicle(vdgi.getPlayer(), vdgi.getDeed());
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				if (creature != null)
					storeMounts(creature);
				break;    // Store mounts when logging out
			default:
				break;
		}
	}
	
	private void generateVehicle(Player player, SWGObject deed) {
		String deedTemplate = deed.getTemplate();
		String pcdTemplate = pcdForVehicleDeed(deedTemplate);
		CreatureObject creator = player.getCreatureObject();
		IntangibleObject vehicleControlDevice = (IntangibleObject) ObjectCreator.createObjectFromTemplate(pcdTemplate);
		
		DestroyObjectIntent.broadcast(deed);
		
		vehicleControlDevice.setServerAttribute(ServerAttribute.PCD_PET_TEMPLATE, mobileForVehicleDeed(deedTemplate));
		vehicleControlDevice.setCount(IntangibleObject.COUNT_PCD_STORED);
		vehicleControlDevice.moveToContainer(creator.getDatapad());
		ObjectCreatedIntent.broadcast(vehicleControlDevice);
		
		callMount(creator, vehicleControlDevice);	// Once generated, the vehicle is called
	}
	
	private void callMount(CreatureObject caller, IntangibleObject mountControlDevice) {
		if (caller.getParent() != null || caller.isInCombat()) {
			return;
		}
		if (caller.getDatapad() != mountControlDevice.getParent()) {
			Log.w("Disconnecting %s - attempted to call mount of another player [%s]", caller.getOwner(), mountControlDevice.getOwner());
			CloseConnectionIntent.broadcast(caller.getOwner(), DisconnectReason.SUSPECTED_HACK);
			return;
		}
		
		String template = mountControlDevice.getServerTextAttribute(ServerAttribute.PCD_PET_TEMPLATE);
		assert template != null : "mount control device doesn't have vehicle template attribute";
		CreatureObject vehicle = (CreatureObject) ObjectCreator.createObjectFromTemplate(template);
		vehicle.systemMove(null, caller.getLocation());
		vehicle.addOptionFlags(OptionFlag.MOUNT);	// The vehicle won't appear properly if this isn't set
		vehicle.setPvpFaction(caller.getPvpFaction());
		vehicle.setOwnerId(caller.getObjectId());	// Client crash if this isn't set before making anyone aware
		
		// TODO after combat there's a delay
		// TODO update faction status on mount if necessary
		
		Mount mount = new Mount(mountControlDevice, vehicle);
		if (mountControlDevice.getCount() == IntangibleObject.COUNT_PCD_CALLED || !calledMounts.computeIfAbsent(caller, c -> new CopyOnWriteArraySet<>()).add(mount)) {
			Log.d("Not calling mount, already called [%s]", caller);
			return;
		}
		mountControlDevice.setCount(IntangibleObject.COUNT_PCD_CALLED);
		
		ObjectCreatedIntent.broadcast(vehicle);
		Log.t("Called mount %s for %s to %s", mount.getMount(), caller, caller.getLocation());
	}
	
	private void enterMount(Player player, CreatureObject vehicle) {
		if (!isMountable(vehicle) || vehicle.getParent() != null) {
			Log.d("%s attempted to mount %s but it's not mountable", player, vehicle);
			return;
		}
		
		CreatureObject requester = player.getCreatureObject();
		
		if (requester.getParent() != null) {
			Log.d("%s attempted to mount a vehicle when inside a container", requester);
			return;
		}
		
		if (requester.getObjectId() == vehicle.getOwnerId()) {
			requester.setLocation(Location.zero());
			requester.moveToSlot(vehicle, "rider", vehicle.getArrangementId(requester));
		} else if (vehicle.getSlottedObject("rider") != null) {
			boolean added = false;
			for (int i = 1; i <= 7; i++) {
				if (!vehicle.hasSlot("rider" + i)) {
					Log.d("%s attempted to mount a vehicle when no slots remain", requester);
					return;
				}
				if (vehicle.getSlottedObject("rider" + i) == null) {
					requester.setLocation(Location.zero());
					requester.moveToSlot(vehicle, "rider" + i, vehicle.getArrangementId(requester));
					added = true;
					break;
				}
			}
			if (!added) {
				Log.d("%s attempted to mount a vehicle when no slots remain", requester);
				return;
			}
		}
		
		requester.setStatesBitmask(CreatureState.RIDING_MOUNT);
		vehicle.setPosture(Posture.DRIVING_VEHICLE);    // TODO RIDING_CREATURE for animals
		vehicle.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		
		requester.inheritMovement(vehicle);
		Log.d("%s mounted %s", requester, vehicle);
	}
	
	private void exitMount(Player player, CreatureObject vehicle) {
		if (!isMountable(vehicle)) {
			Log.d("%s attempted to dismount %s but it's not mountable", player, vehicle);
			return;
		}
		
		CreatureObject requester = player.getCreatureObject();
		
		boolean primary = vehicle.getSlottedObject("rider") == requester;
		requester.moveToContainer(null);
		requester.clearStatesBitmask(CreatureState.RIDING_MOUNT);
		if (primary) {
			for (SWGObject child : vehicle.getSlottedObjects()) {
				assert child instanceof CreatureObject;
				((CreatureObject) child).resetMovement();
				((CreatureObject) child).clearStatesBitmask(CreatureState.RIDING_MOUNT);
				child.moveToContainer(null, vehicle.getLocation());
			}
			vehicle.setPosture(Posture.UPRIGHT);
			vehicle.clearStatesBitmask(CreatureState.MOUNTED_CREATURE);
		}
		
		requester.resetMovement();
		Log.d("%s dismounted %s", requester, vehicle);
	}
	
	private void storeMount(@NotNull CreatureObject player, @NotNull CreatureObject mount, @NotNull IntangibleObject mountControlDevice) {
		if (isMountable(mount)) {
			// Dismount anyone riding the mount about to be stored
			Collection<SWGObject> riders = mount.getSlots().values();
			
			for (SWGObject rider : riders) {
				if (rider == null) {
					continue;
				}
				
				exitMount(rider.getOwner(), mount);
			}
		}
		
		Collection<Mount> mounts = calledMounts.get(player);
		if (mounts != null)
			mounts.removeIf(p -> p.getMount().equals(mount));
		calledMounts.entrySet().removeIf(e -> e.getValue().isEmpty());
		
		DestroyObjectIntent.broadcast(mount);
		mountControlDevice.setCount(IntangibleObject.COUNT_PCD_STORED);
		Log.d("Stored mount %s in control device %s", mount, mountControlDevice);
	}
	
	private void storeMounts(CreatureObject player) {
		Collection<Mount> mounts = calledMounts.remove(player);
		if (mounts == null) {
			Log.d("No mounts called for %s, no action taken", player);
			return;
		}
		
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
	
	private boolean isMountable(CreatureObject mount) {
		return mount.hasOptionFlags(OptionFlag.MOUNT);
	}
	
	private boolean isMounted(CreatureObject mount, CreatureObject rider) {
		return mount.getSlots().containsValue(rider);
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
