package com.projectswg.holocore.services.support.npc.spawn;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DynamicSpawnLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.NoSpawnZoneLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.TerrainLevelLoader;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.data.location.ClosestLocationReducer;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class DynamicSpawnService extends Service {
	
	private static final int SPAWN_DISTANCE_TO_PLAYER = 70;	// Spawner is created 70m away from the player and NPCs are spawned around the spawner
	private static final String EGG_TEMPLATE = "object/tangible/ground_spawning/shared_random_spawner.iff";
	
	private final DynamicSpawnLoader dynamicSpawnLoader;
	private final NoSpawnZoneLoader noSpawnZoneLoader;
	private final TerrainLevelLoader terrainLevelLoader;
	private final Map<Terrain, Collection<ActiveSpawn>> activeSpawnMap;
	private final long destroyTimerMs;
	private final long eggsPerArea;
	private final ScheduledThreadPool executor;
	
	public DynamicSpawnService() {
		dynamicSpawnLoader = ServerData.INSTANCE.getDynamicSpawns();
		noSpawnZoneLoader = ServerData.INSTANCE.getNoSpawnZones();
		terrainLevelLoader = ServerData.INSTANCE.getTerrainLevels();
		activeSpawnMap = Collections.synchronizedMap(new HashMap<>());
		long destroyTimer = PswgDatabase.INSTANCE.getConfig().getLong(this, "destroyTimer", 600);	// Dynamic NPCs are despawned after 10 mins of inactivity
		destroyTimerMs = TimeUnit.MILLISECONDS.convert(destroyTimer, TimeUnit.SECONDS);
		eggsPerArea = PswgDatabase.INSTANCE.getConfig().getLong(this, "eggsPerArea", 4);	// Amount of spawns in an area
		executor = new ScheduledThreadPool(1, "dynamic-spawn-service");
	}
	
	@Override
	public boolean start() {
		long checkRate = 1000;	// Attempt to delete old NPCs every 1000ms
		executor.start();
		executor.executeWithFixedRate(checkRate, checkRate, this::destroyOldNpcs);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		return super.stop() && executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handlePlayerTransformed(PlayerTransformedIntent intent) {
		Location location = intent.getNewLocation();
		
		updateTimestamps(location);
		spawnNewNpcs(intent.getPlayer(), location);
	}
	
	private void updateTimestamps(Location location) {
		Terrain terrain = location.getTerrain();
		Collection<ActiveSpawn> activeSpawns = activeSpawnMap.get(terrain);
		
		if (activeSpawns == null || activeSpawns.isEmpty()) {
			// No active spawns for this terrain. Do nothing.
			return;
		}
		
		for (ActiveSpawn activeSpawn : activeSpawns) {
			SWGObject spawnerObject = activeSpawn.getEggObject();
			Set<Player> observers = spawnerObject.getObservers();
			
			if (!observers.isEmpty()) {
				activeSpawn.setLastSeenTS(System.currentTimeMillis());
			}
		}
	}
	
	private void spawnNewNpcs(CreatureObject player, Location location) {
		Terrain terrain = location.getTerrain();
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> spawnInfos = dynamicSpawnLoader.getSpawnInfos(terrain);
		
		if (spawnInfos.isEmpty()) {
			// There's nothing we can spawn on this planet. Do nothing.
			return;
		}
		
		if (noSpawnZoneLoader.isInNoSpawnZone(location)) {
			// The player is in a no spawn zone. Don't spawn anything.
			return;
		}
		
		TerrainLevelLoader.TerrainLevelInfo terrainLevelInfo = terrainLevelLoader.getTerrainLevelInfo(terrain);
		
		if (terrainLevelInfo == null) {
			// Terrain has no level range defined
			return;
		}
		
		// Find closest no spawn zone
		Collection<NoSpawnZoneLoader.NoSpawnZoneInfo> noSpawnZoneInfos = noSpawnZoneLoader.getNoSpawnZoneInfos(terrain);
		
		if (!noSpawnZoneInfos.isEmpty()) {
			Optional<Location> closestZoneOpt = noSpawnZoneInfos.stream()
					.map(noSpawnZoneInfo -> Location.builder()
							.setX(noSpawnZoneInfo.getX())
							.setZ(noSpawnZoneInfo.getZ())
							.setTerrain(location.getTerrain())
							.build())
					.reduce(new ClosestLocationReducer(location));
			
			Location closestZoneLocation = closestZoneOpt.get();
			
			boolean tooCloseToNoSpawnZone = location.isWithinFlatDistance(closestZoneLocation, SPAWN_DISTANCE_TO_PLAYER);
			
			if (tooCloseToNoSpawnZone) {
				// Player is too close to a no spawn zone. Don't spawn anything.
				return;
			}
		}
		
		long nearbyEggs = player.getAware().stream()
				.filter(swgObject -> EGG_TEMPLATE.equals(swgObject.getTemplate()))
				.count();
		
		if (nearbyEggs >= eggsPerArea) {
			// Plenty spawns near this player already - do nothing
			return;
		}
		
		// Spawn the egg
		ThreadLocalRandom random = ThreadLocalRandom.current();
		boolean usePositiveDirectionX = random.nextBoolean();
		boolean usePositiveDirectionZ = random.nextBoolean();
		double eggX = (usePositiveDirectionX ? SPAWN_DISTANCE_TO_PLAYER : -SPAWN_DISTANCE_TO_PLAYER) + location.getX();
		double eggZ = (usePositiveDirectionZ ? SPAWN_DISTANCE_TO_PLAYER : -SPAWN_DISTANCE_TO_PLAYER) + location.getZ();
		SWGObject eggObject = ObjectCreator.createObjectFromTemplate(EGG_TEMPLATE);
		Location eggLocation = Location.builder(location)
				.setX(eggX)
				.setZ(eggZ)
				.build();	// TODO y parameter should be set and calculated based on X and Z in relevant terrain. Currently they'll spawn in the air or below ground.
		eggObject.moveToContainer(null, eggLocation);	// Spawn egg in the world
		ObjectCreatedIntent.broadcast(eggObject);
		int randomSpawnInfoIndex = random.nextInt(0, spawnInfos.size());
		DynamicSpawnLoader.DynamicSpawnInfo spawnInfo = new ArrayList<>(spawnInfos).get(randomSpawnInfoIndex);
		eggObject.setObjectName(spawnInfo.getDynamicId());
		
		long minLevel = terrainLevelInfo.getMinLevel();
		long maxLevel = terrainLevelInfo.getMaxLevel();
		
		// TODO spawn (loitering?) NPCs within the terrain level range up to 32m away from the egg
		
		Collection<ActiveSpawn> terrainActiveSpawns = activeSpawnMap.computeIfAbsent(terrain, k -> new ArrayList<>());
		terrainActiveSpawns.add(new ActiveSpawn(eggObject, Collections.emptyList()));
	}
	
	private void destroyOldNpcs() {
		Collection<Collection<ActiveSpawn>> globalActiveSpawns = activeSpawnMap.values();
		
		for (Collection<ActiveSpawn> activeSpawns : globalActiveSpawns) {
			for (ActiveSpawn activeSpawn : new ArrayList<>(activeSpawns)) {
				long lastSeenTS = activeSpawn.getLastSeenTS();
				long nowTS = System.currentTimeMillis();
				long delta = nowTS - lastSeenTS;
				
				SWGObject eggObject = activeSpawn.getEggObject();
				
				boolean noPlayersNearby = !eggObject.getObservers().isEmpty();
				
				if (delta >= destroyTimerMs && noPlayersNearby) {
					// It's been too long since an active player last saw this spawn and no player is nearby - destroy it
					if (activeSpawns.remove(activeSpawn)) {
						Collection<TangibleObject> npcs = activeSpawn.getNpcs();
						
						DestroyObjectIntent.broadcast(eggObject);
						
						for (TangibleObject npc : npcs) {
							DestroyObjectIntent.broadcast(npc);
						}
						Log.d("Destroyed inactive dynamic spawn at " + eggObject.getWorldLocation());
					}
				}
			}
		}
	}
	
	private static class ActiveSpawn {
		private final SWGObject eggObject;
		private final Collection<TangibleObject> npcs;
		
		private long lastSeenTS;	// Timestamp in millis for when this object was last viewed by a player
		
		public ActiveSpawn(SWGObject eggObject, Collection<TangibleObject> npcs) {
			this.eggObject = eggObject;
			this.npcs = npcs;
			lastSeenTS = System.currentTimeMillis();
		}
		
		public SWGObject getEggObject() {
			return eggObject;
		}
		
		public Collection<TangibleObject> getNpcs() {
			return npcs;
		}
		
		public long getLastSeenTS() {
			return lastSeenTS;
		}
		
		public void setLastSeenTS(long lastSeenTS) {
			this.lastSeenTS = lastSeenTS;
		}
	}
}
