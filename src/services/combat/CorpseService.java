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

import intents.BuffIntent;
import intents.FactionIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatBroadcastIntent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.PlayClientEffectObjectMessage;
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
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import resources.Location;
import resources.Posture;
import resources.PvpFaction;
import resources.PvpStatus;
import resources.Terrain;
import resources.client_info.ClientFactory;
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
import resources.server_info.StandardLog;
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
	
	private static final String DB_QUERY = "SELECT * FROM cloning_respawn";
	private static final byte CLONE_TIMER = 30;	// Amount of minutes before a player is forced to clone
	
	private final ScheduledExecutorService executor;
	private final Map<CreatureObject, Future<?>> reviveTimers;
	private final Map<String, FacilityData> facilityDataMap;
	private final List<BuildingObject> cloningFacilities;
	private final Random random;
	
	public CorpseService() {
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("corpse-service"));
		reviveTimers = new HashMap<>();
		facilityDataMap = new HashMap<>();
		cloningFacilities = new ArrayList<>();
		random = new Random();
		
		registerForIntent(CreatureKilledIntent.class, cki -> handleCreatureKilledIntent(cki));
		registerForIntent(ObjectCreatedIntent.class, oci -> handleObjectCreatedIntent(oci));
		registerForIntent(DestroyObjectIntent.class, doi -> handleDestroyObjectIntent(doi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		
		loadFacilityData();
	}

	@Override
	public boolean terminate() {
		executor.shutdown();
		return super.terminate();
	}
	
	private void loadFacilityData() {
		long startTime = StandardLog.onStartLoad("cloning facility data");
		loadRespawnData();
		StandardLog.onEndLoad(facilityDataMap.size(), "cloning facility data", startTime);
	}

	private void loadRespawnData() {
		try (RelationalDatabase respawnDatabase = RelationalServerFactory.getServerData("cloning/cloning_respawn.db", "cloning_respawn")) {
			try (ResultSet set = respawnDatabase.executeQuery(DB_QUERY)) {
				while (set.next()) {
					int tubeCount = set.getInt("tubes");
					TubeData[] tubeData = new TubeData[tubeCount];

					for (int i = 1; i <= tubeCount; i++) {
						String tubeName = "tube" + i;
						tubeData[i - 1] = new TubeData(set.getFloat(tubeName + "_x"), set.getFloat(tubeName + "_z"), set.getFloat(tubeName + "_heading"));
					}

					String stfCellValue = set.getString("stf_name");
					String stfName = stfCellValue.equals("-") ? null : stfCellValue;
					PvpFaction factionRestriction = null;

					switch (stfCellValue) {
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
						Log.e("Duplicate entry for %s in row %d. Replacing previous entry with new", objectTemplate, set.getRow());
					}
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		
		if(corpse.isPlayer()) {
			Player corpseOwner = corpse.getOwner();
			new ChatBroadcastIntent(corpseOwner, new ProsePackage(new StringId("base_player", "prose_victim_dead"), "TT", i.getKiller().getObjectName())).broadcast();
			new ChatBroadcastIntent(corpseOwner, new ProsePackage(new StringId("base_player", "revive_exp_msg"), "TT", CLONE_TIMER + " minutes.")).broadcast();
			
			scheduleCloneTimer(corpse);
		} else {
			// This is a NPC - schedule corpse for deletion
			executor.schedule(() -> deleteCorpse(corpse), 120, TimeUnit.SECONDS);
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject createdObject = i.getObject();
		
		if(!(createdObject instanceof BuildingObject)) {
			return;
		}
		
		BuildingObject createdBuilding = (BuildingObject) createdObject;
		String objectTemplate = createdBuilding.getTemplate();
		
		if(facilityDataMap.containsKey(objectTemplate)) {
			synchronized(cloningFacilities) {
				cloningFacilities.add(createdBuilding);
			}
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		synchronized(cloningFacilities) {
			SWGObject destroyedObject = i.getObject();
			
			if(!(destroyedObject instanceof BuildingObject)) {
				return;
			}
			
			cloningFacilities.remove((BuildingObject) destroyedObject);
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		switch(i.getEvent()) {
			case PE_DISAPPEAR: {
				CreatureObject creature = i.getPlayer().getCreatureObject();
				Future<?> reviveTimer = reviveTimers.remove(creature);
				
				if (reviveTimer != null) {
					// They had an active timer when they disappeared
					reviveTimer.cancel(false);
				}
				break;
			}
			
			case PE_FIRST_ZONE: {
				CreatureObject creature = i.getPlayer().getCreatureObject();

				if (creature.getPosture() == Posture.DEAD && !reviveTimers.containsKey(creature)) {
					// They're dead but they have no active revive timer.
					// In this case, they didn't clone before the application was shut down and started back up.
					scheduleCloneTimer(creature);
				}
				break;
			}
			default:
				break;
		}
	}
	
	private void scheduleCloneTimer(CreatureObject corpse) {
		Terrain corpseTerrain = corpse.getTerrain();
		List<BuildingObject> availableFacilities = getAvailableFacilities(corpse);
		
		if (availableFacilities.isEmpty()) {
			Log.e("No cloning facility is available for terrain %s - %s has nowhere to properly clone", corpseTerrain, corpse);
			return;
		}

		SuiWindow cloningWindow = createSuiWindow(availableFacilities, corpse);

		cloningWindow.display(corpse.getOwner());
		synchronized (reviveTimers) {
			reviveTimers.put(corpse, executor.schedule(() -> expireCloneTimer(corpse, availableFacilities, cloningWindow), CLONE_TIMER, TimeUnit.MINUTES));
		}
	}
	
	private SuiWindow createSuiWindow(List<BuildingObject> availableFacilities, CreatureObject corpse) {
		SuiListBox suiWindow = new SuiListBox(SuiButtons.OK, "@base_player:revive_title", "@base_player:clone_prompt_header");
		
		for (BuildingObject cloningFacility : availableFacilities) {
			FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());
			String stfName = facilityData.getStfName();

			suiWindow.addListItem(stfName != null ? stfName : cloningFacility.getCurrentCity());
		}

		suiWindow.addCallback("handleFacilityChoice", (Player player, SWGObject actor, SuiEvent event, Map<String, String> parameters) -> {
			int selectionIndex = SuiListBox.getSelectedRow(parameters);

			if (event != SuiEvent.OK_PRESSED || selectionIndex >= availableFacilities.size() || selectionIndex < 0) {
				suiWindow.display(player);
				return;
			}

			if (reviveCorpse(corpse, availableFacilities.get(selectionIndex)) != CloneResult.SUCCESS) {
				suiWindow.display(player);
			}
		});

		return suiWindow;
	}
	
	/**
	 * Only used for NPCs!
	 * @param creatureCorpse non-player creature to delete from the world
	 */
	private void deleteCorpse(CreatureObject creatureCorpse) {
		if(creatureCorpse.isPlayer()) {
			Log.e("Cannot delete the corpse of a player!", creatureCorpse);
		} else {
			new DestroyObjectIntent(creatureCorpse).broadcast();
			Log.i("Corpse of NPC %s was deleted from the world", creatureCorpse);
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
					.sorted((facility, otherFacility) -> Double.compare(corpseLocation.distanceTo(facility.getLocation()), corpseLocation.distanceTo(otherFacility.getLocation())))
					.collect(Collectors.toList());
			
		}
	}
	
	// TODO below doesn't apply to a a player that died in a heroic. Cloning on Dathomir should be possible if you die during the Axkva Min heroic.
	private boolean isValidTerrain(BuildingObject cloningFacility, CreatureObject corpse)  {
		return cloningFacility.getTerrain() == corpse.getTerrain();
	}
	
	private boolean isFactionAllowed(BuildingObject cloningFacility, CreatureObject corpse) {
		FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());
		PvpFaction factionRestriction = facilityData.getFactionRestriction();
		
		return factionRestriction == null || factionRestriction == corpse.getPvpFaction();
	}
	
	private CloneResult reviveCorpse(CreatureObject corpse, BuildingObject selectedFacility) {
		FacilityData facilityData = facilityDataMap.get(selectedFacility.getTemplate());

		if (facilityData == null) {
			Log.e("%s could not clone at facility %s because the object template is not in cloning_respawn.sdb", corpse, selectedFacility);
			return CloneResult.TEMPLATE_MISSING;
		}

		String cellName = facilityData.getCell();
		CellObject cellObject = selectedFacility.getCellByName(cellName);

		if (cellObject == null) {
			Log.e("Cell %s was invalid for cloning facility %s", cellName, selectedFacility);
			return CloneResult.INVALID_CELL;
		}
		
		// Cancel the forced cloning timer
		synchronized (reviveTimers) {
			Future<?> timer = reviveTimers.remove(corpse);
			
			if (timer != null) {
				timer.cancel(false);
			}
		}
		
		teleport(corpse, cellObject, getCloneLocation(facilityData, selectedFacility));
		return CloneResult.SUCCESS;
	}
	
	private Location getCloneLocation(FacilityData facilityData, BuildingObject selectedFacility) {
		Location cloneLocation;
		Location facilityLocation = selectedFacility.getLocation();
		TubeData[] tubeData = facilityData.getTubeData();
		int tubeCount = tubeData.length;

		if (tubeCount > 0) {
			TubeData randomData = tubeData[random.nextInt(tubeCount)];
			cloneLocation = new Location(randomData.getTubeX(), 0, randomData.getTubeZ(), facilityLocation.getTerrain());
			cloneLocation.setOrientation(facilityLocation.getOrientationX(), facilityLocation.getOrientationY(), facilityLocation.getOrientationZ(), facilityLocation.getOrientationW());
			cloneLocation.rotateHeading(randomData.getTubeHeading());
		} else {
			cloneLocation = new Location(facilityData.getX(), facilityData.getY(), facilityData.getZ(), facilityLocation.getTerrain());
			cloneLocation.rotateHeading(facilityData.getHeading());
		}
		
		return cloneLocation;
	}
	
	private void teleport(CreatureObject corpse, CellObject cellObject, Location cloneLocation) {
		if (corpse.getPvpFaction() != PvpFaction.NEUTRAL) {
			new FactionIntent(corpse, PvpStatus.ONLEAVE).broadcast();
		}
		
		new ObjectTeleportIntent(corpse, cellObject, cloneLocation).broadcast();
		corpse.setPosture(Posture.UPRIGHT);
		corpse.setTurnScale(1);
		corpse.setMovementScale(1);
		corpse.setHealth(corpse.getMaxHealth());
		corpse.sendObserversAndSelf(new PlayClientEffectObjectMessage("clienteffect/player_clone_compile.cef", "", corpse.getObjectId()));
		
		BuffIntent cloningSickness = new BuffIntent("cloning_sickness", corpse, corpse, false);
		new BuffIntent("incapweaken", corpse, corpse, true).broadcastAfterIntent(cloningSickness);
		cloningSickness.broadcast();
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
		
			new ChatBroadcastIntent(corpseOwner, "@base_player:revive_expired").broadcast();
			suiWindow.close(corpseOwner);
			forceClone(corpse, facilitiesInTerrain);
		} else {
			Log.w("Could not expire timer for %s because none was active", corpse);
		}
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
