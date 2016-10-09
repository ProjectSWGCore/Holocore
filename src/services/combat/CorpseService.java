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
package services.combat;

import intents.FactionIntent;
import intents.chat.ChatBroadcastIntent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.combat.CreatureKilledIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import resources.Location;
import resources.Posture;
import resources.PvpFaction;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;
import resources.sui.SuiButtons;
import resources.sui.SuiEvent;
import resources.sui.SuiListBox;
import resources.sui.SuiWindow;
import utilities.ThreadUtilities;

/**
 * The {@code CorpseService} removes corpses from the world a while after
 * they've died. It also lets players clone at a cloning facility.
 * @author mads
 */
public final class CorpseService extends Service {
	
	private static final byte CLONE_TIMER = 30;	// Amount of minutes before a player is forced to clone
	
	private final ScheduledExecutorService executor;
	private final Map<String, FacilityData> facilityDataMap;
	private final Map<CloneMapping, CloneMapping> cloneMapping;	// Needed for dungeons that have no cloning facilities
	private final List<BuildingObject> cloningFacilities;
	private final Random random;
	
	public CorpseService() {
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("corpse-service"));
		facilityDataMap = new HashMap<>();
		cloneMapping = new HashMap<>();
		cloningFacilities = new ArrayList<>();
		random = new Random();
		
		registerForIntent(CreatureKilledIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
		
		loadFacilityData();
	}

	@Override
	public boolean terminate() {
		executor.shutdown();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case CreatureKilledIntent.TYPE: handleCreatureKilledIntent((CreatureKilledIntent) i); break;
			case ObjectCreatedIntent.TYPE: handleObjectCreatedIntent((ObjectCreatedIntent) i); break;
			case DestroyObjectIntent.TYPE: handleDestroyObjectIntent((DestroyObjectIntent) i); break;
		}
	}
	
	private void loadFacilityData() {
		long startTime = System.currentTimeMillis();
		Log.i(this, "Loading cloning facility data...");
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("cloning/cloning_mapping.db", "cloning_mapping")) {
			try (ResultSet set = spawnerDatabase.executeQuery("SELECT * FROM cloning_mapping")) {
				while (set.next()) {
					String terrainName;
					String areaName;
					String targetTerrainName;
					String targetAreaName;
				}
			} catch (SQLException e) {
				Log.e(this, e);
			}
		}
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("cloning/cloning_respawn.db", "cloning_respawn")) {
			try (ResultSet set = spawnerDatabase.executeQuery("SELECT * FROM cloning_respawn")) {
				while (set.next()) {
					int tubeCount = set.getInt("tubes");
					TubeData[] tubeData = new TubeData[tubeCount];
					
					for (int i = 1; i <= tubeCount; i++) {
						String tubeName = "tube" + i;
						tubeData[i-1] = new TubeData(set.getFloat(tubeName + "_x"), set.getFloat(tubeName + "_z"), set.getFloat(tubeName + "_heading"));
					}
					
					String stfCellValue = set.getString("stf_name");
					String stfName = stfCellValue.equals("-") ? null : stfCellValue;
					PvpFaction factionRestriction = null;
					
					switch(stfCellValue) {
						case "FACTION_REBEL":
							factionRestriction = PvpFaction.REBEL;
							break;
						case "FACTION_IMPERIAL":
							factionRestriction = PvpFaction.IMPERIAL;
							break;
					}
					
					FacilityData facilityData = new FacilityData(factionRestriction, set.getFloat("x"), set.getFloat("y"), set.getFloat("z"), set.getString("cell"), FacilityType.valueOf(set.getString("clone_type")), stfName, set.getInt("heading"), tubeData);
					String objectTemplate = set.getString("structure");
					
					if (facilityDataMap.put(ClientFactory.formatToSharedFile(objectTemplate), facilityData) != null) {
						// Duplicates are not allowed!
						Log.e(this, "Duplicate entry for %s in row %d. Replacing previous entry with new", objectTemplate, set.getRow());
					}
				}
			} catch (SQLException e) {
				Log.e(this, e);
			}
		}
		
		Log.i(this, "Finished loading cloning facility data for %d object templates. Time: %dms", facilityDataMap.size(), System.currentTimeMillis() - startTime);
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getKilledCreature();
		
		if(corpse.isPlayer()) {
			Player corpseOwner = corpse.getOwner();
			new ChatBroadcastIntent(corpseOwner, new ProsePackage(new StringId("base_player", "prose_victim_dead"), "TT", i.getKillerCreature().getName())).broadcast();
			new ChatBroadcastIntent(corpseOwner, new ProsePackage(new StringId("base_player", "revive_exp_msg"), "TT", CLONE_TIMER + " minutes.")).broadcast();
			
			Terrain destinationTerrain = corpse.getTerrain();	// TODO get from terrainMapping
			List<BuildingObject> availableFacilities = getAvailableFacilities(corpse, destinationTerrain);
			
			if (!availableFacilities.isEmpty()) {
				SuiListBox suiWindow = new SuiListBox(SuiButtons.OK, "@base_player:revive_title", "@base_player:clone_prompt_header");
				// Add options to SUI window
				for (BuildingObject cloningFacility : availableFacilities) {
					FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());

					String stfName = facilityData.getStfName();
					
					if(stfName != null) {
						suiWindow.addListItem(stfName);
					} else {
						suiWindow.addListItem(cloningFacility.getCurrentCity());
					}
				}

				suiWindow.addCallback("handleFacilityChoice", (Player player, SWGObject actor, SuiEvent event, Map<String, String> parameters) -> {
					int selectionIndex = SuiListBox.getSelectedRow(parameters);

					if (event != SuiEvent.OK_PRESSED || selectionIndex + 1 > availableFacilities.size() || selectionIndex < 0) {
						suiWindow.display(player);
						return;
					}

					switch(clone(corpse, availableFacilities.get(selectionIndex))) {
						case INVALID_SELECTION:
							// TODO system message
							suiWindow.display(player);
							break;
						case INVALID_CELL:
							// TODO system message
							suiWindow.display(player);
							break;
						case TEMPLATE_MISSING:
							// TODO system message
							suiWindow.display(player);
							break;
						case SUCCESS:
							// TODO do anything special here? If not, delete case
							break;
					}
				});

				suiWindow.display(corpseOwner);
				
				executor.schedule(() -> expireCloneTimer(corpse, availableFacilities, suiWindow), CLONE_TIMER, TimeUnit.MINUTES);
			} else {
				// TODO no cloners available! Wat do?
				Log.e(this, "No cloning facility is available for terrain %s - %s has nowhere to properly clone", destinationTerrain, corpse);
			}
		} else {
			// This is a NPC - schedule corpse for deletion
			executor.schedule(() -> deleteCorpse(corpse), 120, TimeUnit.SECONDS);
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject createdObject = i.getObject();
		if(createdObject instanceof BuildingObject) {
			BuildingObject createdBuilding = (BuildingObject) createdObject;
			String objectTemplate = createdBuilding.getTemplate();
			
			if(facilityDataMap.containsKey(objectTemplate)) {
				synchronized(cloningFacilities) {
					cloningFacilities.add(createdBuilding);
				}
			}
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		synchronized(cloningFacilities) {
			SWGObject destroyedObject = i.getObject();
			
			if(destroyedObject instanceof BuildingObject && cloningFacilities.remove((BuildingObject) destroyedObject)) {
				Log.d(this, "Cloning facility %s was destroyed", destroyedObject);
			}
		}
	}
	
	/**
	 * Only used for NPCs!
	 * @param creatureCorpse non-player creature to delete from the world
	 */
	private void deleteCorpse(CreatureObject creatureCorpse) {
		if(creatureCorpse.isPlayer()) {
			Log.e(this, "Cannot delete the corpse of a player!", creatureCorpse);
		} else {
			new DestroyObjectIntent(creatureCorpse).broadcast();
			Log.i(this, "Corpse of NPC %s was deleted from the world", creatureCorpse);
		}
	}
	
	/**
	 * 
	 * @param corpse
	 * @return a sorted list of {@code BuildingObject}, ordered by distance
	 * to {@code corpse}. Order is reversed, so the closest facility is
	 * first.
	 */
	private List<BuildingObject> getAvailableFacilities(CreatureObject corpse, Terrain destinationTerrain) {
		
		synchronized (cloningFacilities) {
			Location corpseLocation = corpse.getWorldLocation();
			return cloningFacilities.stream()
					.filter(facilityObject -> isValidTerrain(facilityObject, destinationTerrain) && isFactionAllowed(facilityObject, corpse))
					.sorted((facility, otherFacility) -> Double.compare(corpseLocation.distanceTo(facility.getLocation()), corpseLocation.distanceTo(otherFacility.getLocation())))
					.collect(Collectors.toList());
			
		}
	}
	
	private boolean isValidTerrain(BuildingObject cloningFacility, Terrain destinationTerrain)  {
		return cloningFacility.getTerrain() == destinationTerrain;
	}
	
	private boolean isFactionAllowed(BuildingObject cloningFacility, CreatureObject corpse) {
		FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());
		PvpFaction factionRestriction = facilityData.getFactionRestriction();
		
		return factionRestriction == null || factionRestriction == corpse.getPvpFaction();
	}
	
	private CloneResult clone(CreatureObject cloneRequestor, BuildingObject selectedFacility) {
		Location facilityLocation = selectedFacility.getLocation();
		FacilityData facilityData = facilityDataMap.get(selectedFacility.getTemplate());

		if (facilityData != null) {
			String cellName = facilityData.getCell();
			CellObject cellObject = selectedFacility.getCellByName(cellName);

			if (cellObject != null) {
				// TODO They should go on leave upon cloning
//				if(creature.getPvpFaction() != PvpFaction.NEUTRAL)
//					new FactionIntent(creature, PvpStatus.ONLEAVE).broadcast();

				TubeData[] tubeData = facilityData.getTubeData();

				int tubeCount = tubeData.length;

				Location cloneLocation;

				if (tubeCount > 0) {
					TubeData randomData = tubeData[random.nextInt(tubeCount)];
					cloneLocation = new Location(randomData.getTubeX(), 0, randomData.getTubeZ(), facilityLocation.getTerrain());
					cloneLocation.setOrientation(facilityLocation.getOrientationX(), facilityLocation.getOrientationY(), facilityLocation.getOrientationZ(), facilityLocation.getOrientationW());

					// The creature should point towards the entrance/exit of the tube
					cloneLocation.rotateHeading(randomData.getTubeHeading());
				} else {
					cloneLocation = new Location(facilityData.getX(), facilityData.getY(), facilityData.getZ(), facilityLocation.getTerrain());
					cloneLocation.rotateHeading(facilityData.getHeading());
				}

				new ObjectTeleportIntent(cloneRequestor, cellObject, cloneLocation).broadcast();
				cloneRequestor.setPosture(Posture.UPRIGHT);
				cloneRequestor.setTurnScale(1);
				cloneRequestor.setMovementScale(1);
				cloneRequestor.setHealth(cloneRequestor.getMaxHealth());
				
				// TODO NGE: cloning debuff
				
				return CloneResult.SUCCESS;
			} else {
				Log.e(this, "Cell %s was invalid for cloning facility %s", cellName, selectedFacility);
				return CloneResult.INVALID_CELL;
			}
		} else {
			Log.e(this, "%s could not clone at facility %s because the object template is not in cloning_respawn.sdb", cloneRequestor, selectedFacility);
			return CloneResult.TEMPLATE_MISSING;
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
			if (clone(cloneRequestor, facility) == CloneResult.SUCCESS) {
				return true;
			}
		}
		
		return false;
	}
	
	private void expireCloneTimer(CreatureObject corpse, List<BuildingObject> facilitiesInTerrain, SuiWindow suiWindow) {
		Player corpseOwner = corpse.getOwner();
		
		new ChatBroadcastIntent(corpseOwner, "@base_player:revive_expired").broadcast();
		suiWindow.close(corpseOwner);
		forceClone(corpse, facilitiesInTerrain);
	}
	
	private static class FacilityData {
		private final PvpFaction factionRestriction;
		private final float x, y, z;
		private final String cell;
		private final FacilityType facilityType;
		private final String stfName;
		private final int heading;
		private final TubeData[] tubeData;

		public FacilityData(PvpFaction factionRestriction, float x, float y, float z, String cell, FacilityType facilityType, String stfName, int tubeHeading, TubeData[] tubeData) {
			this.factionRestriction = factionRestriction;
			this.x = x;
			this.y = y;
			this.z = z;
			this.cell = cell;
			this.facilityType = facilityType;
			this.stfName = stfName;
			this.heading = tubeHeading;
			this.tubeData = tubeData;
		}

		public PvpFaction getFactionRestriction() {
			return factionRestriction;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public float getZ() {
			return z;
		}

		public String getCell() {
			return cell;
		}

		public FacilityType getFacilityType() {
			return facilityType;
		}

		public String getStfName() {
			return stfName;
		}

		public int getHeading() {
			return heading;
		}

		public TubeData[] getTubeData() {
			return tubeData;
		}
	}
	
	private static enum FacilityType {
		STANDARD,
		RESTRICTED,
		PLAYER_CITY,
		CAMP,
		PRIVATE_INSTANCE,
		FACTION_IMPERIAL,
		FACTION_REBEL,
		PVP_REGION_ADVANCED_IMPERIAL,
		PVP_REGION_ADVANCED_REBEL
	}
	
	private static class TubeData {
		private final float tubeX, tubeZ, tubeHeading;

		public TubeData(float tubeX, float tubeZ, float tubeHeading) {
			this.tubeX = tubeX;
			this.tubeZ = tubeZ;
			this.tubeHeading = tubeHeading;
		}

		public float getTubeX() {
			return tubeX;
		}

		public float getTubeZ() {
			return tubeZ;
		}

		public float getTubeHeading() {
			return tubeHeading;
		}
	}
	
	private static enum CloneResult {
		INVALID_SELECTION, TEMPLATE_MISSING, INVALID_CELL, SUCCESS
	}
}