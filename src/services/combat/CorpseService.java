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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import resources.Location;
import resources.Posture;
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
import utilities.ThreadUtilities;

/**
 * The {@code CorpseService} removes corpses from the world a while after
 * they've died. It also lets players clone at a cloning facility.
 * @author mads
 */
public final class CorpseService extends Service {
	
	private final ScheduledExecutorService executor;
	private final Map<String, FacilityData> facilityDataMap;
	private final List<BuildingObject> cloningFacilities;
	private final Random random;
	
	public CorpseService() {
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("corpse-service"));
		facilityDataMap = new HashMap<>();
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
		switch(i.getType()) {
			case CreatureKilledIntent.TYPE: handleCreatureKilledIntent((CreatureKilledIntent) i); break;
			case ObjectCreatedIntent.TYPE: handleObjectCreatedIntent((ObjectCreatedIntent) i); break;
			case DestroyObjectIntent.TYPE: handleDestroyObjectIntent((DestroyObjectIntent) i); break;
		}
	}
	
	private void loadFacilityData() {
		long startTime = System.currentTimeMillis();
		Log.i(this, "Loading cloning facility data...");
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("player/cloning_respawn.db", "cloning_respawn")) {
			try (ResultSet set = spawnerDatabase.executeQuery("SELECT * FROM cloning_respawn")) {
				while (set.next()) {
					int tubeCount = set.getInt("tubes");
					TubeData[] tubeData = new TubeData[tubeCount];
					
					for(int i = 0; i < tubeCount; i++) {
						String tubeName = String.format("tube%d", i + 1);
						tubeData[i] = new TubeData(set.getFloat(tubeName + "_x"), set.getFloat(tubeName + "_z"), set.getFloat(tubeName + "_heading"));
					}
					
					FacilityData facilityData = new FacilityData(set.getFloat("x"), set.getFloat("y"), set.getFloat("z"), set.getString("cell"), FacilityType.valueOf(set.getString("clone_type")), set.getString("stf_name"), tubeData);
					String objectTemplate = set.getString("structure");
					
					if(facilityDataMap.put(objectTemplate, facilityData) != null) {
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
		CreatureObject killedCreature = i.getKilledCreature();
		
		if(killedCreature.isPlayer()) {
			Player killedCreatureOwner = killedCreature.getOwner();
			new ChatBroadcastIntent(killedCreatureOwner, new ProsePackage(new StringId("base_player", "prose_victim_dead"), "TT", i.getKillerCreature().getName())).broadcast();
			// TODO periodically remind the deathblown player that their timer is expiring. How often?
				// TODO after 30 minutes, close the SUI window and force them to clone at the nearest cloning facility
			SuiListBox window = new SuiListBox(SuiButtons.OK, "@base_player:revive_title", "@base_player:clone_prompt_header");

			Collection<BuildingObject> facilitiesInTerrain = new ArrayList<>();

			// Loop over clone facilities
			for (BuildingObject cloningFacility : cloningFacilities) {
				// A player can only clone into facilities in the same terrain as they're in themselves
				if (cloningFacility.getLocation().getTerrain() == killedCreature.getLocation().getTerrain()) {
					facilitiesInTerrain.add(cloningFacility);
				}
			}

			if (facilitiesInTerrain.isEmpty()) {
				// Add options to SUI window
				for (BuildingObject cloningFacility : facilitiesInTerrain) {
					FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());

					// TODO not all facilities use this
					window.addListItem(facilityData.getStfName());
				}

				// TODO put below into a method pls
				window.addCallback("handleFacilityChoice", (Player player, SWGObject actor, SuiEvent event, Map<String, String> parameters) -> {
					int selectionIndex = SuiListBox.getSelectedRow(parameters);

					if (event != SuiEvent.OK_PRESSED || selectionIndex + 1 > cloningFacilities.size() || selectionIndex < 0) {
						window.display(player);
						return;
					}

					synchronized (cloningFacilities) {
						BuildingObject selectedFacility = cloningFacilities.get(selectionIndex);

						FacilityData facilityData = facilityDataMap.get(selectedFacility.getTemplate());

						if (facilityData != null) {
							CreatureObject creature = player.getCreatureObject();
							String cellName = facilityData.getCell();
							CellObject cellObject = selectedFacility.getCellByName(cellName);

							if (cellObject != null) {
								TubeData[] tubeData = facilityData.getTubeData();

								TubeData randomData = tubeData[random.nextInt(tubeData.length)];
								Location tubeLocation = new Location(randomData.getTubeX(), 0, randomData.getTubeZ(), selectedFacility.getLocation().getTerrain());

								new ObjectTeleportIntent(creature, cellObject, tubeLocation).broadcast();

								creature.setPosture(Posture.UPRIGHT);
							} else {
								Log.e(this, "Cell %s was invalid for cloning facility %s", cellName, selectedFacility);
								// TODO system message to player
							}
							// TODO NGE: cloning debuff
						} else {
							Log.e(this, "%s could not clone at facility %s because the object template is not in cloning_respawn.sdb", actor, selectedFacility);
							// TODO system message to player
						}
					}
				});

				window.display(killedCreatureOwner);
			} else {
				
			}
		} else {
			// This is a NPC - schedule corpse for deletion
			executor.schedule(() -> deleteCorpse(killedCreature), 120, TimeUnit.SECONDS);
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject createdObject = i.getObject();
		if(createdObject instanceof BuildingObject) {
			BuildingObject createdBuilding = (BuildingObject) createdObject;
			String objectTemplate = createdBuilding.getTemplate();

			FacilityData facilityData = facilityDataMap.get(objectTemplate);

			// If null, this object template isn't in cloning_respawn, which is fine
			if(facilityData != null) {
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
	
	private static class FacilityData {
		private final float x, y, z;
		private final String cell;
		private final FacilityType facilityType;
		private final String stfName;
		private final TubeData[] tubeData;

		public FacilityData(float x, float y, float z, String cell, FacilityType facilityType, String stfName, TubeData[] tubeData) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.cell = cell;
			this.facilityType = facilityType;
			this.stfName = stfName;
			this.tubeData = tubeData;
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

		public TubeData[] getTubeData() {
			return tubeData;
		}
	}
	
	private static enum FacilityType {
		STANDARD,
		RESTRICTED,
		PLAYER_CITY,
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
	
}