/***********************************************************************************
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
package com.projectswg.holocore.services.galaxy;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.debug.Log;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.intents.object.ObjectTeleportIntent;
import com.projectswg.holocore.intents.pet.PetCallIntent;
import com.projectswg.holocore.intents.pet.PetMountIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.creature.CreatureState;
import com.projectswg.holocore.resources.objects.tangible.OptionFlag;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.objects.ObjectCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mads on 19-06-2017.
 */
class ControlDeviceService extends Service {	// TODO rename to ControlDeviceService
	
	// TODO no-vehicle zones (does not affect creature mounts)
	
	private final Map<CreatureObject, Collection<Pet>> calledPets;	// TODO rename Pet class and this field
	
	ControlDeviceService() {
		calledPets = new HashMap<>();
		
		registerForIntent(ObjectTeleportIntent.class, this::handleObjectTeleport);
		registerForIntent(PetCallIntent.class, this::handlePetCall);
		registerForIntent(PetMountIntent.class, this::handlePetMount);
		registerForIntent(PlayerEventIntent.class, this::handlePlayerEvent);
	}
	
	@Override
	public boolean stop() {
		globallyStorePets();	// Don't want pets out and about when server starts up again
		
		return super.stop();
	}
	
	String mobileForVehicleDeed(String deedTemplate) {
		return deedTemplate.replace("_deed", "").replace("tangible/deed", "mobile");
	}
	
	String pcdForVehicleDeed(String deedTemplate) {
		return deedTemplate.replace("tangible/deed/vehicle_deed", "intangible/vehicle").replace("deed", "pcd");
	}
	
	private void handleObjectTeleport(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		
		if (!(object instanceof CreatureObject)) {
			return;
		}
		
		storePets((CreatureObject) object);
	}
	
	private void handlePetMount(PetMountIntent pmi) {
		enterMount(pmi.getPlayer(), pmi.getPet());
	}
	
	private void handlePetCall(PetCallIntent pci) {
		CreatureObject creature = pci.getPlayer().getCreatureObject();
		
		callPet(creature, pci.getControlDevice(), creature.getLocation());
	}
	
	private void handlePlayerEvent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				storePets(pei.getPlayer().getCreatureObject());
				break;    // Store pets when logging out
			case PE_FIRST_ZONE:	// TODO debugging
				SWGObject deed = ObjectCreator.createObjectFromTemplate("object/tangible/deed/vehicle_deed/shared_speederbike_flash_deed.iff");
				ObjectCreatedIntent.broadcast(deed);
				generateVehicle(pei.getPlayer(), deed);
				break;
		}
	}
	
	private void generateVehicle(Player player, SWGObject deed) {
		String deedTemplate = deed.getTemplate();
		String mobileTemplate = mobileForVehicleDeed(deedTemplate);
		String pcdTemplate = pcdForVehicleDeed(deedTemplate);
		CreatureObject creator = player.getCreatureObject();
		CreatureObject vehicle = (CreatureObject) ObjectCreator.createObjectFromTemplate(mobileTemplate);
		SWGObject vehicleControlDevice = ObjectCreator.createObjectFromTemplate(pcdTemplate);
		
		DestroyObjectIntent.broadcast(deed);
		
		Log.d("Vehicle with mobile template %s generated by %s", mobileTemplate, creator);
		
		vehicle.addOptionFlags(OptionFlag.MOUNT);	// The vehicle won't appear properly if this isn't set
		vehicle.setOwnerId(creator.getObjectId());	// Client crash if this isn't set before making anyone aware
		vehicle.moveToContainer(vehicleControlDevice);
		vehicleControlDevice.moveToContainer(creator.getSlottedObject("datapad"));
		callPet(creator, vehicle, creator.getLocation());	// Once generated, the vehicle is called
		ObjectCreatedIntent.broadcast(vehicle);
		ObjectCreatedIntent.broadcast(vehicleControlDevice);
	}
	
	private void enterMount(Player player, CreatureObject vehicle) {
		if (!isMountable(vehicle)) {
			Log.d("%s attempted to mount %s but it's not mountable", player, vehicle);
			return;
		}
		
		CreatureObject requester = player.getCreatureObject();
		
		// TODO check if they're mounted in general instead
		if (isMounted(vehicle, requester)) {
			Log.d("%s attempted to mount an object that they are already mounted on", requester);
			return;
		}
		
		requester.moveToContainer(vehicle);    // Put requester in the vehicle object
		requester.setStatesBitmask(CreatureState.RIDING_MOUNT);
		
		vehicle.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.DRIVING_VEHICLE);    // TODO RIDING_CREATURE for animals
		
		// TODO add Vehicle speed, turn, acceleration etc. These are set on the requester
	}
	
	private void exitMount(Player player, CreatureObject vehicle) {
		if (!isMountable(vehicle)) {
			Log.d("%s attempted to dismount %s but it's not mountable", player, vehicle);
			return;
		}
		
		CreatureObject requester = player.getCreatureObject();
		
		requester.moveToContainer(null);    // Put requester back in the world
		requester.clearStatesBitmask(CreatureState.RIDING_MOUNT);
		
		// TODO only execute below if the mount is now empty?
		// TODO if the one dismounting is also the one driving, everyone else should be dismounted
		vehicle.clearStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.UPRIGHT);
		
		// TODO remove Vehicle speed, turn, acceleration etc from requester
	}
	
	private void callPet(CreatureObject caller, SWGObject petControlDevice, Location location) {
		Collection<Pet> callerPets;
		
		if (calledPets.containsKey(caller)) {
			callerPets = calledPets.get(caller);
			
			// TODO check if size of callerPets exceeds max allowed.
		} else {
			callerPets = new ArrayList<>();
			calledPets.put(caller, callerPets);
		}
		
		SWGObject pcdInventory = petControlDevice.getSlottedObject("inventory");
		Collection<SWGObject> pcdPets = petControlDevice.getContainedObjects();
		
		for (SWGObject pet : pcdPets) {
			callerPets.add(new Pet(pcdInventory, (CreatureObject) pet));
			// TODO no calling during combat
			// TODO after combat there's a delay
			pet.setLocation(location);
			// TODO update faction status on pet if necessary
			// TODO check if ownerId equals object ID of the caller. If not, set it
			pet.moveToContainer(null);    // Move pet from Pet Control Device into the world
		}
	}
	
	/**
	 * Stores the specified pet
	 */
	private void storePet(CreatureObject pet, SWGObject petControlDevice) {
		if (isMountable(pet)) {
			// Dismount anyone riding the pet about to be stored
			Collection<SWGObject> riders = pet.getSlots().values();
			
			for (SWGObject rider : riders) {
				if (rider == null) {
					continue;
				}
				
				exitMount(rider.getOwner(), pet);
			}
		}
		
		pet.moveToContainer(petControlDevice);	// Transfer pet from awareness to the datapad control device
		Log.v("Stored pet %s in control device %s", pet, petControlDevice);
	}
	
	/**
	 * Stores any pets that might be called by the specified player
	 */
	private void storePets(CreatureObject player) {
		if (!calledPets.containsKey(player)) {
			Log.d("No pets called for %s, no action taken", player);
			return;
		}
		
		Collection<Pet> pets = calledPets.remove(player);
		
		for (Pet pet : pets) {
			storePet(pet.getCreature(), pet.getPetControlDevice());
		}
	}
	
	/**
	 * Stores all called pets by all players
	 */
	private void globallyStorePets() {
		calledPets.keySet().forEach(this::storePets);
	}
	
	private boolean isMountable(CreatureObject pet) {
		return pet.hasOptionFlags(OptionFlag.MOUNT);
	}
	
	private boolean isMounted(CreatureObject mount, CreatureObject rider) {
		return mount.getSlots().containsValue(rider);
	}
	
	private static class Pet {
		
		private final SWGObject petControlDevice;
		private final CreatureObject creature;
		
		private Pet(SWGObject petControlDevice, CreatureObject creature) {
			this.petControlDevice = petControlDevice;
			this.creature = creature;
		}
		
		private SWGObject getPetControlDevice() {
			return petControlDevice;
		}
		
		private CreatureObject getCreature() {
			return creature;
		}
		
	}
	
}
