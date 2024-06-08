/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat.cloning;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.FacilityData;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.TubeData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CloningService extends Service {
	
	private static final long CLONE_TIMER = 30;	// Amount of minutes before a player is forced to clone
	private final Map<CreatureObject, Future<?>> reviveTimers;
	private final List<BuildingObject> cloningFacilities;
	private final ScheduledThreadPool executor;
	
	public CloningService() {
		this.reviveTimers = new HashMap<>();
		this.cloningFacilities = new ArrayList<>();
		this.executor = new ScheduledThreadPool(1, "combat-cloning-service");
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
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		if (!corpse.isPlayer())
			return;
		
		Player corpseOwner = corpse.getOwner();
		if (corpseOwner != null) {
			new SystemMessageIntent(corpseOwner, new ProsePackage(new StringId("base_player", "prose_victim_dead"), "TT", i.getKiller().getObjectName())).broadcast();
			new SystemMessageIntent(corpseOwner, new ProsePackage(new StringId("base_player", "revive_exp_msg"), "TT", CLONE_TIMER + " minutes.")).broadcast();
		}
		
		scheduleCloneTimer(corpse);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject createdObject = i.getObject();
		
		if(!(createdObject instanceof BuildingObject createdBuilding)) {
			return;
		}
		
		String objectTemplate = createdBuilding.getTemplate();
		FacilityData facility = ServerData.INSTANCE.getCloningFacilities().getFacility(objectTemplate);
		if(facility != null) {
			synchronized(cloningFacilities) {
				cloningFacilities.add(createdBuilding);
			}
		}
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		synchronized(cloningFacilities) {
			SWGObject destroyedObject = i.getObject();
			
			if(!(destroyedObject instanceof BuildingObject)) {
				return;
			}
			
			cloningFacilities.remove(destroyedObject);
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		CreatureObject creature = i.getPlayer().getCreatureObject();
		switch(i.getEvent()) {
			case PE_DISAPPEAR: {
				Future<?> reviveTimer = reviveTimers.remove(creature);
				
				if (reviveTimer != null) {
					// They had an active timer when they disappeared
					reviveTimer.cancel(false);
				}
				break;
			}
			
			case PE_FIRST_ZONE:
				if (creature.getPosture() == Posture.DEAD) {
					// They're dead but they have no active revive timer.
					// In this case, they didn't clone before the application was shut down and started back up.
					if (reviveTimers.containsKey(creature))
						showSuiWindow(creature);
					else
						scheduleCloneTimer(creature);
				}
				break;
			default:
				break;
		}
	}
	
	private void scheduleCloneTimer(CreatureObject corpse) {
		List<BuildingObject> availableFacilities = getAvailableFacilities(corpse);
		if (availableFacilities.isEmpty()) {
			BuildingObject defaultCloner = getDefaultCloner();
			if (defaultCloner != null)
				availableFacilities.add(defaultCloner);
		}
		
		SuiWindow cloningWindow = createSuiWindow(availableFacilities, corpse);

		cloningWindow.display(corpse.getOwner());
		synchronized (reviveTimers) {
			reviveTimers.put(corpse, executor.execute(TimeUnit.MINUTES.toMillis(CLONE_TIMER), () -> expireCloneTimer(corpse, availableFacilities, cloningWindow)));
		}
	}
	
	private void showSuiWindow(CreatureObject corpse) {
		List<BuildingObject> availableFacilities = getAvailableFacilities(corpse);
		
		if (availableFacilities.isEmpty()) {
			BuildingObject defaultCloner = getDefaultCloner();
			if (defaultCloner != null)
				availableFacilities.add(defaultCloner);
		}
		
		createSuiWindow(availableFacilities, corpse).display(corpse.getOwner());
	}
	
	private SuiWindow createSuiWindow(List<BuildingObject> availableFacilities, CreatureObject corpse) {
		BuildingObject closestFacility = availableFacilities.get(0);

		String preDesignated = "Pre-Designated: None";
		String cashBalance = "Cash Balance: " + corpse.getCashBalance();
		String help = "\nSelect the desired operation and click OK";
		String prompt = String.join("\n", preDesignated, cashBalance, help);
		SuiListBox suiWindow = new SuiListBox(SuiButtons.OK, "@base_player:revive_title", prompt);
		suiWindow.addListItem("@base_player:revive_closest");
		
		suiWindow.addCallback("handleFacilityChoice", (SuiEvent event, Map<String, String> parameters) -> {
			int selectionIndex = SuiListBox.getSelectedRow(parameters);
			
			if (event != SuiEvent.OK_PRESSED || selectionIndex >= availableFacilities.size() || selectionIndex < 0) {
				suiWindow.display(corpse.getOwner());
				return;
			}
			
			if (reviveCorpse(corpse, closestFacility) != CloneResult.SUCCESS) {
				suiWindow.display(corpse.getOwner());
			}
		});

		return suiWindow;
	}
	
	private CloneResult reviveCorpse(CreatureObject corpse, BuildingObject selectedFacility) {
		FacilityData facilityData = ServerData.INSTANCE.getCloningFacilities().getFacility(selectedFacility.getTemplate());

		if (facilityData == null) {
			StandardLog.onPlayerError(this, corpse, "could not clone at facility %s because the object template is not in cloning_respawn.sdb", selectedFacility);
			return CloneResult.TEMPLATE_MISSING;
		}

		String cellName = facilityData.getCell();
		CellObject cellObject = selectedFacility.getCellByName(cellName);

		if (cellObject == null) {
			StandardLog.onPlayerError(this, corpse, "could not clone at facility %s because the target cell is invalid", selectedFacility);
			return CloneResult.INVALID_CELL;
		}
		
		// Cancel the forced cloning timer
		synchronized (reviveTimers) {
			Future<?> timer = reviveTimers.remove(corpse);
			
			if (timer != null) {
				timer.cancel(false);
			}
		}
		
		StandardLog.onPlayerEvent(this, corpse, "cloned to %s @ %s", selectedFacility, selectedFacility.getLocation());
		teleport(corpse, cellObject, getCloneLocation(facilityData, selectedFacility));
		return CloneResult.SUCCESS;
	}
	
	private Location getCloneLocation(FacilityData facilityData, BuildingObject selectedFacility) {
		LocationBuilder cloneLocation = Location.builder();
		Location facilityLocation = selectedFacility.getLocation();
		List<TubeData> tubeData = new ArrayList<>(facilityData.getTubes());
		int tubeCount = tubeData.size();

		if (tubeCount > 0) {
			TubeData randomData = tubeData.get(ThreadLocalRandom.current().nextInt(tubeCount));
			cloneLocation.setTerrain(facilityLocation.getTerrain());
			cloneLocation.setPosition(randomData.getTubeX(), 0, randomData.getTubeZ());
			cloneLocation.setOrientation(facilityLocation.getOrientationX(), facilityLocation.getOrientationY(), facilityLocation.getOrientationZ(), facilityLocation.getOrientationW());
			cloneLocation.rotateHeading(randomData.getTubeHeading());
		} else {
			cloneLocation.setTerrain(facilityLocation.getTerrain());
			cloneLocation.setPosition(facilityData.getX(), facilityData.getY(), facilityData.getZ());
			cloneLocation.rotateHeading(facilityData.getHeading());
		}
		
		return cloneLocation.build();
	}
	
	private void teleport(CreatureObject corpse, CellObject cellObject, Location cloneLocation) {
		corpse.moveToContainer(cellObject, cloneLocation);
		corpse.setPosture(Posture.UPRIGHT);
		corpse.setTurnScale(1);
		corpse.setMovementPercent(1);
		corpse.setHealth(corpse.getMaxHealth());
		corpse.sendObservers(new PlayClientEffectObjectMessage("clienteffect/player_clone_compile.cef", "", corpse.getObjectId(), ""));
		corpse.sendSelf(new PlayMusicMessage(0, "sound/item_repairobj.snd", 1, false));
		if (corpse.getPvpFaction() != PvpFaction.NEUTRAL) {
			corpse.broadcast(new UpdateFactionStatusIntent(corpse, PvpStatus.ONLEAVE));
		}
	}
	
	/**
	 * Picks the closest cloning facility and clones {@code cloneRequestor}
	 * there. If an error occurs upon attempting to clone, it will pick the next
	 * facility until no errors occur. If allAlso closes the cloning SUI window.
	 * @param cloneRequestor
	 * @param facilitiesInTerrain list of {@code BuildingObject} that represent
	 * in-game cloning facilities
	 * @return {@code true} if forceful cloning was succesful and {@code false}
	 * if {@code cloneRequestor} cannot be cloned at any of the given facilities
	 * in {@code facilitiesInTerrain}
	 */
	private boolean forceClone(CreatureObject cloneRequestor, List<BuildingObject> facilitiesInTerrain) {
		for (BuildingObject facility : facilitiesInTerrain) {
			if (reviveCorpse(cloneRequestor, facility) == CloneResult.SUCCESS) {
				return true;
			}
		}
		
		return false;
	}
	
	private void expireCloneTimer(CreatureObject corpse, List<BuildingObject> facilitiesInTerrain, SuiWindow suiWindow) {
		if(reviveTimers.remove(corpse) != null) {
			Player corpseOwner = corpse.getOwner();
		
			if (corpseOwner != null) {
				new SystemMessageIntent(corpseOwner, "@base_player:revive_expired").broadcast();
				suiWindow.close(corpseOwner);
			}
			
			forceClone(corpse, facilitiesInTerrain);
		} else {
			StandardLog.onPlayerError(this, corpse, "could not be force cloned because no timer was active");
		}
	}
	
	/**
	 * 
	 * @param corpse
	 * @return a sorted list of {@code BuildingObject}, ordered by distance
	 * to {@code corpse}. Order is reversed, so the closest facility is
	 * first.
	 */
	private List<BuildingObject> getAvailableFacilities(CreatureObject corpse) {
		synchronized (cloningFacilities) {
			Location corpseLocation = corpse.getWorldLocation();
			return cloningFacilities.stream()
					.filter(facilityObject -> isValidTerrain(facilityObject, corpse) && isFactionAllowed(facilityObject, corpse))
					.sorted(Comparator.comparingDouble(facility -> corpseLocation.distanceTo(facility.getLocation())))
					.collect(Collectors.toList());
			
		}
	}
	
	// TODO below doesn't apply to a a player that died in a heroic. Cloning on Dathomir should be possible if you die during the Axkva Min heroic.
	private boolean isValidTerrain(BuildingObject cloningFacility, CreatureObject corpse)  {
		return cloningFacility.getTerrain() == corpse.getTerrain();
	}
	
	private boolean isFactionAllowed(BuildingObject cloningFacility, CreatureObject corpse) {
		FacilityData facilityData = ServerData.INSTANCE.getCloningFacilities().getFacility(cloningFacility.getTemplate());
		PvpFaction factionRestriction = facilityData.getFactionRestriction();
		
		return factionRestriction == null || factionRestriction == corpse.getPvpFaction();
	}
	
	private static BuildingObject getDefaultCloner() {
		BuildingObject defaultCloner = BuildingLookup.getBuildingByTag("tat_moseisley_cloning1");
		if (defaultCloner == null)
			Log.e("No default cloner found with building id: 'tat_moseisley_cloning1'");
		
		return defaultCloner;
	}

	private enum CloneResult {
		INVALID_SELECTION, TEMPLATE_MISSING, INVALID_CELL, SUCCESS
	}
	
}