package com.projectswg.holocore.services.gameplay.combat.cloning;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CloningService extends Service {
	
	private static final String DB_QUERY = "SELECT * FROM cloning_respawn";
	private static final long CLONE_TIMER = 30;	// Amount of minutes before a player is forced to clone
	
	private final Map<CreatureObject, Future<?>> reviveTimers;
	private final Map<String, FacilityData> facilityDataMap;
	private final List<BuildingObject> cloningFacilities;
	private final ScheduledThreadPool executor;
	
	public CloningService() {
		this.reviveTimers = new HashMap<>();
		this.facilityDataMap = new HashMap<>();
		this.cloningFacilities = new ArrayList<>();
		this.executor = new ScheduledThreadPool(1, "combat-cloning-service");
	}
	
	@Override
	public boolean initialize() {
		loadFacilityData();
		return true;
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
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		if (!corpse.isPlayer())
			return;
		
		Player corpseOwner = corpse.getOwner();
		new SystemMessageIntent(corpseOwner, new ProsePackage(new StringId("base_player", "prose_victim_dead"), "TT", i.getKiller().getObjectName())).broadcast();
		new SystemMessageIntent(corpseOwner, new ProsePackage(new StringId("base_player", "revive_exp_msg"), "TT", CLONE_TIMER + " minutes.")).broadcast();
		
		scheduleCloneTimer(corpse);
	}
	
	@IntentHandler
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
		SuiListBox suiWindow = new SuiListBox(SuiButtons.OK, "@base_player:revive_title", "@base_player:clone_prompt_header");
		
		for (BuildingObject cloningFacility : availableFacilities) {
			FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());
			String name;
			
			if (facilityData.getStfName() != null)
				name = facilityData.getStfName();
			else if (!cloningFacility.getCurrentCity().isEmpty())
				name = cloningFacility.getCurrentCity();
			else
				name = String.format("%s[%d, %d]", cloningFacility.getTerrain(), (int) cloningFacility.getX(), (int) cloningFacility.getZ());
			
			suiWindow.addListItem(name);
		}
		
		suiWindow.addCallback("handleFacilityChoice", (SuiEvent event, Map<String, String> parameters) -> {
			int selectionIndex = SuiListBox.getSelectedRow(parameters);

			if (event != SuiEvent.OK_PRESSED || selectionIndex >= availableFacilities.size() || selectionIndex < 0) {
				suiWindow.display(corpse.getOwner());
				return;
			}

			if (reviveCorpse(corpse, availableFacilities.get(selectionIndex)) != CloneResult.SUCCESS) {
				suiWindow.display(corpse.getOwner());
			}
		});

		return suiWindow;
	}
	
	private CloneResult reviveCorpse(CreatureObject corpse, BuildingObject selectedFacility) {
		FacilityData facilityData = facilityDataMap.get(selectedFacility.getTemplate());

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
		
		// We're put on leave when we're revived at a cloning facility
		new FactionIntent(corpse, PvpStatus.ONLEAVE).broadcast();
		
		StandardLog.onPlayerEvent(this, corpse, "cloned to %s @ %s", selectedFacility, selectedFacility.getLocation());
		teleport(corpse, cellObject, getCloneLocation(facilityData, selectedFacility));
		return CloneResult.SUCCESS;
	}
	
	private Location getCloneLocation(FacilityData facilityData, BuildingObject selectedFacility) {
		LocationBuilder cloneLocation = Location.builder();
		Location facilityLocation = selectedFacility.getLocation();
		TubeData[] tubeData = facilityData.getTubeData();
		int tubeCount = tubeData.length;

		if (tubeCount > 0) {
			TubeData randomData = tubeData[ThreadLocalRandom.current().nextInt(tubeCount)];
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
		if (corpse.getPvpFaction() != PvpFaction.NEUTRAL) {
			new FactionIntent(corpse, PvpStatus.ONLEAVE).broadcast();
		}
		
		corpse.setPosture(Posture.UPRIGHT);
		corpse.setTurnScale(1);
		corpse.setMovementScale(1);
		corpse.setHealth(corpse.getMaxHealth());
		corpse.sendObservers(new PlayClientEffectObjectMessage("clienteffect/player_clone_compile.cef", "", corpse.getObjectId(), ""));
		corpse.broadcast(new BuffIntent("cloning_sickness", corpse, corpse, false));
		corpse.broadcast(new BuffIntent("incapWeaken", corpse, corpse, true));
		corpse.moveToContainer(cellObject, cloneLocation);
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
		
			new SystemMessageIntent(corpseOwner, "@base_player:revive_expired").broadcast();
			suiWindow.close(corpseOwner);
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
		FacilityData facilityData = facilityDataMap.get(cloningFacility.getTemplate());
		PvpFaction factionRestriction = facilityData.getFactionRestriction();
		
		return factionRestriction == null || factionRestriction == corpse.getPvpFaction();
	}
	
	private static BuildingObject getDefaultCloner() {
		long clonerId = DataLoader.buildings().getBuilding("tat_moseisley_cloning1").getId();
		BuildingObject defaultCloner = (clonerId == 0) ? null : (BuildingObject) ObjectLookup.getObjectById(clonerId);
		if (defaultCloner == null)
			Log.e("No default cloner found with building id: 'tat_moseisley_cloning1'");
		
		return defaultCloner;
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
	
	private enum FacilityType {
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
	
	private enum CloneResult {
		INVALID_SELECTION, TEMPLATE_MISSING, INVALID_CELL, SUCCESS
	}
	
}
