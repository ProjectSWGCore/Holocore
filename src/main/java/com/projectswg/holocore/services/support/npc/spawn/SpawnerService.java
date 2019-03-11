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
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.CreateClientPathMessage;
import com.projectswg.common.network.packets.swg.zone.DestroyClientPathMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.StaticSpawnInfo;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint;
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.permissions.AdminPermissions;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SpawnerService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Set<CreatureObject> adminsWithRoutes;
	
	public SpawnerService() {
		this.executor = new ScheduledThreadPool(1, "spawner-service");
		this.adminsWithRoutes = ConcurrentHashMap.newKeySet();
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		if (PswgDatabase.config().getBoolean(this, "spawnEggsEnabled", true))
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
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		CreatureObject player = ipi.getPlayer().getCreatureObject();
		SWGPacket packet = ipi.getPacket();
		if (!(packet instanceof IntendedTarget) || player == null)
			return;
		long intendedTargetId = ((IntendedTarget) packet).getTargetId();
		SWGObject intendedTarget = ObjectLookup.getObjectById(intendedTargetId);
		if (intendedTarget == null && adminsWithRoutes.remove(player)) {
			player.sendSelf(new DestroyClientPathMessage());
		} else if (intendedTarget != null && player.hasCommand("admin")) {
			Spawner spawner = (Spawner) intendedTarget.getServerAttribute(ServerAttribute.EGG_SPAWNER);
			if (spawner != null) {
				List<ResolvedPatrolWaypoint> waypoints = spawner.getPatrolRoute();
				player.sendSelf(new CreateClientPathMessage(waypoints.stream()
						.map(wayp -> wayp.getParent() == null ? wayp.getLocation() : Location.builder(wayp.getLocation()).translateLocation(wayp.getParent().getWorldLocation()).build())
						.map(Location::getPosition)
						.collect(Collectors.toList())));
				adminsWithRoutes.add(player);
			}
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject player = pei.getPlayer().getCreatureObject();
		if (pei.getEvent() == PlayerEvent.PE_LOGGED_OUT && player != null && adminsWithRoutes.remove(player))
			player.sendSelf(new DestroyClientPathMessage());
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
				Log.e("Failed to load spawner[%s]/npc[%s]. %s: %s", spawn.getId(), spawn.getNpcId(), t.getClass().getName(), t.getMessage());
			}
		}
		
		StandardLog.onEndLoad(count, "spawners", startTime);
	}
	
	private void spawn(StaticSpawnInfo spawn) {
		SWGObject egg = createEgg(spawn);
		Spawner spawner = new Spawner(spawn, egg);
		egg.setServerAttribute(ServerAttribute.EGG_SPAWNER, spawner);
		
		for (int i = 0; i < spawner.getAmount(); i++) {
			NPCCreator.createNPC(spawner);
		}
		
		List<ResolvedPatrolWaypoint> patrolRoute = spawner.getPatrolRoute();
		if (patrolRoute != null) {
			for (ResolvedPatrolWaypoint waypoint : patrolRoute) {
				SWGObject obj = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/patrol_waypoint.iff");
				obj.setContainerPermissions(AdminPermissions.getPermissions());
				obj.setServerAttribute(ServerAttribute.EGG_SPAWNER, spawner);
				//noinspection HardcodedLineSeparator
				obj.setObjectName(String.format("P: %s\nG: %s\nNPC:%s\nID:%s", waypoint.getPatrolId(), waypoint.getGroupId(), spawn.getNpcId(), spawn.getId()));
				obj.moveToContainer(waypoint.getParent(), waypoint.getLocation());
				ObjectCreatedIntent.broadcast(obj);
			}
		}
	}
	
	@SuppressWarnings("HardcodedLineSeparator")
	private static SWGObject createEgg(StaticSpawnInfo spawn) {
		SpawnerType spawnerType = SpawnerType.valueOf(spawn.getSpawnerType());
		SWGObject egg = ObjectCreator.createObjectFromTemplate(spawnerType.getObjectTemplate());
		egg.setContainerPermissions(AdminPermissions.getPermissions());
		if (spawn.getPatrolId().isEmpty() || spawn.getPatrolId().equals("0"))
			egg.setObjectName(String.format("%s\nNPC: %s", spawn.getId(), spawn.getNpcId()));
		else
			egg.setObjectName(String.format("%s\nNPC: %s\nG: %s", spawn.getId(), spawn.getNpcId(), spawn.getPatrolId()));
		egg.systemMove(getCell(spawn.getId(), spawn.getCellId(), spawn.getBuildingId()), Location.builder().setTerrain(spawn.getTerrain()).setPosition(spawn.getX(), spawn.getY(), spawn.getZ()).setHeading(spawn.getHeading()).build());
		ObjectCreatedIntent.broadcast(egg);
		return egg;
	}
	
	private static SWGObject getCell(String spawnId, int cellId, String buildingTag) {
		if (buildingTag.isEmpty() || buildingTag.endsWith("_world"))
			return null;
		BuildingObject building = BuildingLookup.getBuildingByTag(buildingTag);
		if (building == null) {
			Log.w("Skipping spawner with ID %s - building_id %s didn't reference a BuildingObject!", spawnId, buildingTag);
			return null;
		}
		
		SWGObject cellObject = building.getCellByNumber(cellId);
		if (cellObject == null) {
			Log.e("Spawner with ID %s - building %s didn't have cell ID %d!", spawnId, buildingTag, cellId);
		}
		return cellObject;
	}
	
}
