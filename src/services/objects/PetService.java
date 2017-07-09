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
package services.objects;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.debug.Log;
import intents.ObjectTeleportedIntent;
import intents.PlayerEventIntent;
import intents.object.ContainerTransferIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.Posture;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.creature.CreatureState;
import resources.objects.tangible.OptionFlag;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.radial.RadialOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mads on 19-06-2017.
 */
public final class PetService extends Service {

	// TODO no-vehicle zones (does not affect creature mounts)

	private final Map<CreatureObject, Collection<Pet>> calledPets;


	public PetService() {
		calledPets = new HashMap<>();

		registerForIntent(ObjectTeleportIntent.class, oti -> handleObjectTeleport(oti));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEvent(pei));
		registerForIntent(RadialRequestIntent.class, rri -> handleRadialRequest(rri));
		registerForIntent(RadialSelectionIntent.class, rsi -> handleRadialSelection(rsi));
	}

	private void handleObjectTeleport(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();

		if (!(object instanceof CreatureObject)) {
			return;
		}

		storePets((CreatureObject) object);
	}

	private void handlePlayerEvent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT: storePets(pei.getPlayer().getCreatureObject()); break;	// Store pets when logging out
		}
	}

	@Override
	public boolean terminate() {
		storePets();

		return super.terminate();
	}

	private void handleRadialRequest(RadialRequestIntent rri) {
		SWGObject target = rri.getTarget();
		Player player = rri.getPlayer();
		List<RadialOption> options = new ArrayList<>();

		if (target.getTemplate().startsWith("object/tangible/deed/vehicle_deed/")) {
			// This is a deed
			options.add(new RadialOption(RadialItem.VEHICLE_GENERATE));
		} else if (isMountable(target) && target.getParent() == null) {
			// This is a spawned mount
			CreatureObject vehicle = (CreatureObject) target;
			CreatureObject requester  = player.getCreatureObject();
			long ownerId = vehicle.getOwnerId();

			if (ownerId != requester.getObjectId()) {	// Check ownership
				return;
			}

			if (vehicle.getSlots().containsValue(requester)) {
				System.out.println("Exit vehicle");
				// Requester is on this mount
				options.add(new RadialOption(RadialItem.SERVER_VEHICLE_EXIT));
			} else {
				System.out.println("Enter vehicle");
				options.add(new RadialOption(RadialItem.SERVER_VEHICLE_ENTER));
			}
		} else if (target.getTemplate().startsWith("object/intangible/vehicle/")) {
			// This is a Pet Control Device

			// TODO call or store radial, check containedObjects of target
		} else {
			return;
		}


		new RadialResponseIntent(player, target, options, rri.getRequest().getCounter()).broadcast();
	}

	private void handleRadialSelection(RadialSelectionIntent rsi) {
		switch (rsi.getSelection()) {
			case VEHICLE_GENERATE: generateVehicle(rsi.getPlayer(), rsi.getTarget()); break;
			case SERVER_VEHICLE_ENTER: enterMount(rsi.getPlayer(), rsi.getTarget()); break;
			case SERVER_VEHICLE_EXIT: exitMount(rsi.getPlayer(), rsi.getTarget()); break;
		}
	}

	private void generateVehicle(Player player, SWGObject deed) {
		String deedTemplate = deed.getTemplate();
		String mobileTemplate = mobileForVehicleDeed(deedTemplate);
		String pcdTemplate = pcdForVehicleDeed(deedTemplate);
		CreatureObject creator = player.getCreatureObject();
		CreatureObject vehicle = (CreatureObject) ObjectCreator.createObjectFromTemplate(mobileTemplate);

		if (vehicle == null) {
			Log.e("Vehicle not generated: Invalid mobile template %s", mobileTemplate);
			return;
		}

		SWGObject petControlDevice = ObjectCreator.createObjectFromTemplate(pcdTemplate);

		if (petControlDevice == null) {
			Log.e("Vehicle not generated: Invalid PCD template %s", pcdTemplate);
			return;
		}

		new DestroyObjectIntent(deed).broadcast();

		Log.d("Vehicle with mobile template %s generated by %s", mobileTemplate, creator);

		vehicle.addOptionFlags(OptionFlag.MOUNT);	// The vehicle won't appear properly if this isn't set
		vehicle.setOwnerId(creator.getObjectId());	// Client crash if this isn't set before making anyone aware
		vehicle.moveToContainer(petControlDevice.getSlottedObject("inventory"));

		petControlDevice.moveToContainer(creator.getSlottedObject("datapad"));

		callPet(vehicle, creator.getLocation());
		new ObjectCreatedIntent(vehicle).broadcast();
		new ObjectCreatedIntent(petControlDevice).broadcast();
	}

	private void enterMount(Player player, SWGObject object) {
		if (!isMountable(object)) {
			Log.d("%s attempted to mount %s but it's not mountable", player, object);
			return;
		}

		CreatureObject vehicle = (CreatureObject) object;
		CreatureObject requester = player.getCreatureObject();

		requester.moveToContainer(vehicle);	// Put requester in the vehicle object
		requester.setStatesBitmask(CreatureState.RIDING_MOUNT);

		vehicle.setStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.DRIVING_VEHICLE);	// TODO RIDING_CREATURE for animals



		// TODO add Vehicle speed, turn, acceleration etc. These are set on the requester
	}

	private void exitMount(Player player, SWGObject object) {
		if (!isMountable(object)) {
			Log.d("%s attempted to dismount %s but it's not mountable", player, object);
			return;
		}

		CreatureObject vehicle = (CreatureObject) object;
		CreatureObject requester = player.getCreatureObject();

		requester.moveToContainer(null);	// Put requester back in the world
		requester.clearStatesBitmask(CreatureState.RIDING_MOUNT);

		vehicle.clearStatesBitmask(CreatureState.MOUNTED_CREATURE);
		vehicle.setPosture(Posture.UPRIGHT);
		// TODO remove Vehicle speed, turn, acceleration etc from requester
	}

	private String mobileForVehicleDeed(String deedTemplate) {
		return deedTemplate.replace("_deed", "").replace("tangible/deed", "mobile");
	}

	private String pcdForVehicleDeed(String deedTemplate) {
		return deedTemplate.replace("tangible/deed/vehicle_deed", "intangible/vehicle").replace("deed", "pcd");
	}

	private void callPet(CreatureObject caller, CreatureObject pet, Location location) {
		Collection<Pet> callerPets;

		if (calledPets.containsKey(caller)) {
			callerPets = calledPets.get(caller);

			// TODO check if size of callerPets exceeds max allowed
		} else {
			callerPets = new ArrayList<>();
			calledPets.put(caller, callerPets);
		}

		callerPets.add(new Pet(pet.getParent(), pet));
		// TODO no calling during combat
		// TODO after combat there's a delay
		pet.setLocation(location);
		// TODO update faction status on pet if necessary
		// TODO check if ownerId equals object ID of the caller. If not, set it
		pet.moveToContainer(null);	// Move pet from Pet Control Device into the world
	}

	/**
	* Stores the specified pet
	*/
	private void storePet(CreatureObject pet, SWGObject petControlDevice) {
		pet.moveToContainer(petControlDevice);
	}

	/**
	 * Stores any pets that might be called by the specified player
	 */
	private void storePets(CreatureObject player) {	
		if (!calledPets.containsKey(player)) {
			return;
		}

		Collection<Pet> pets = calledPets.remove(player);

		for (Pet pet : pets) {
			pet.getCreature().moveToContainer(pet.getPetControlDevice());
		}
	}

	/**
	* Stores all called pets by all players
	*/
	private void storePets() {
		Collection<CreatureObject> petMasters = calledPets.keySet();

		for (CreatureObject petMaster : petMasters) {
			storePets(petMaster);
		}
	}

	private boolean isMountable(SWGObject object) {
		return object instanceof CreatureObject && ((CreatureObject) object).hasOptionFlags(OptionFlag.MOUNT);
	}

	private static class Pet {
		private final SWGObject petControlDevice;
		private final CreatureObject creature;

		private Pet(SWGObject petControlDevice, CreatureObject creature) {
			this.creature = creature;
			this.pet = pet;
		}

		private SWGObject getPetControlDevice() {
			return petControlDevice;
		}

		private CreatureObject getCreature() {
			return creature;
		}

	}


}
