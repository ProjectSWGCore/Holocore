/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.support.npc.spawn;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcPatrolRouteLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcPatrolRouteLoader.PatrolRouteWaypoint;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcStaticSpawnLoader.StaticSpawnInfo;
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.permissions.AdminPermissions;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public final class SpawnerService extends Service {
	
	private final ScheduledThreadPool executor;
	
	public SpawnerService() {
		this.executor = new ScheduledThreadPool(1, "spawner-service");
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		if (DataManager.getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", true))
			loadSpawners();
		
		return true;
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject destroyedObject = doi.getObject();
		if (!(destroyedObject instanceof AIObject))
			return;
		
		Spawner spawner = ((AIObject) destroyedObject).getSpawner();
		
		if (spawner == null) {
			Log.e("Killed AI object %s has no linked Spawner - it cannot respawn!", destroyedObject);
			return;
		}
		
		executor.execute(spawner.getRespawnDelay() * 1000, () -> NPCCreator.createNPC(spawner));
	}
	
	private void loadSpawners() {
		long startTime = StandardLog.onStartLoad("spawners");
		
		int count = 0;
		for (StaticSpawnInfo spawn : DataLoader.npcStaticSpawns().getSpawns()) {
			try {
				spawn(spawn);
				count++;
			} catch (Throwable t) {
				Log.e("Failed to load spawner. %s: %s", t.getClass().getName(), t.getMessage());
			}
		}
		createPatrolRouteWaypoints();
		
		StandardLog.onEndLoad(count, "spawners", startTime);
	}
	
	private void spawn(StaticSpawnInfo spawn) {
		Spawner spawner = new Spawner(spawn, createEgg(spawn));
		
		for (int i = 0; i < spawner.getAmount(); i++) {
			NPCCreator.createNPC(spawner);
		}
	}
	
	private static SWGObject createEgg(StaticSpawnInfo spawn) {
		SpawnerType spawnerType = SpawnerType.valueOf(spawn.getSpawnerType());
		SWGObject egg = ObjectCreator.createObjectFromTemplate(spawnerType.getObjectTemplate());
		egg.setContainerPermissions(AdminPermissions.getPermissions());
		egg.systemMove(getCell(spawn.getId(), spawn.getCellId(), spawn.getBuildingId()), Location.builder().setTerrain(spawn.getTerrain()).setPosition(spawn.getX(), spawn.getY(), spawn.getZ()).setHeading(spawn.getHeading()).build());
		ObjectCreatedIntent.broadcast(egg);
		return egg;
	}
	
	private static SWGObject getCell(int spawnId, int cellId, String buildingTag) {
		if (buildingTag.isEmpty() || buildingTag.endsWith("_world"))
			return null;
		BuildingObject building = BuildingLookup.getBuildingByTag(buildingTag);
		if (building == null) {
			Log.w("Skipping spawner with ID %d - building_id %s didn't reference a BuildingObject!", spawnId, buildingTag);
			return null;
		}
		
		SWGObject cellObject = building.getCellByNumber(cellId);
		if (cellObject == null) {
			Log.e("Spawner with ID %d - building %s didn't have cell ID %d!", spawnId, buildingTag, cellId);
		}
		return cellObject;
	}
	
	private static void createPatrolRouteWaypoints() {
		NpcPatrolRouteLoader npcPatrolRouteLoader = DataLoader.npcPatrolRoutes();
		npcPatrolRouteLoader.forEach(route -> {
			for (PatrolRouteWaypoint waypoint : route) {
				SWGObject obj = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/patrol_waypoint.iff");
				obj.setContainerPermissions(AdminPermissions.getPermissions());
				obj.moveToContainer(getPatrolWaypointParent(waypoint), getPatrolWaypointLocation(waypoint));
				ObjectCreatedIntent.broadcast(obj);
			}
		});
	}
	
	private static Location getPatrolWaypointLocation(PatrolRouteWaypoint waypoint) {
		return Location.builder()
				.setTerrain(waypoint.getTerrain())
				.setX(waypoint.getX())
				.setY(waypoint.getY())
				.setZ(waypoint.getZ()).build();
	}

	private static SWGObject getPatrolWaypointParent(PatrolRouteWaypoint waypoint) {
		if (waypoint.getBuildingId().isEmpty() || waypoint.getBuildingId().endsWith("_world"))
			return null;
		
		BuildingObject building = BuildingLookup.getBuildingByTag(waypoint.getBuildingId());
		if (building == null) {
			Log.w("PatrolRouteWaypoint: Invalid building [%s] for patrol id: %d and group id: %d", waypoint.getBuildingId(), waypoint.getPatrolId(), waypoint.getGroupId());
			return null;
		}

		SWGObject cell = building.getCellByNumber(waypoint.getCellId());
		if (cell == null) {
			Log.w("PatrolRouteWaypoint: Invalid cell [%d] for building: %s, patrol id: %d and group id: %d", waypoint.getCellId(), waypoint.getBuildingId(), waypoint.getPatrolId(), waypoint.getGroupId());
			return null;
		}

		return cell;
	}
	
}
