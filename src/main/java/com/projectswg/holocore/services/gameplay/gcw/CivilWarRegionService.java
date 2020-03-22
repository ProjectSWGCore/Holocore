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
package com.projectswg.holocore.services.gameplay.gcw;

import com.projectswg.common.data.encodables.gcw.GcwGroup;
import com.projectswg.common.data.encodables.gcw.GcwGroupZone;
import com.projectswg.common.data.encodables.gcw.GcwRegion;
import com.projectswg.common.data.encodables.gcw.GcwRegionZone;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.GcwGroupsRsp;
import com.projectswg.common.network.packets.swg.zone.GcwRegionsReq;
import com.projectswg.common.network.packets.swg.zone.GcwRegionsRsp;
import com.projectswg.holocore.intents.gameplay.gcw.faction.CivilWarPointIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.player.ActivePlayerPredicate;
import com.projectswg.holocore.resources.support.data.server_info.database.PswgGcwRegionDatabase;
import com.projectswg.holocore.resources.support.data.server_info.database.ZoneMetadata;
import com.projectswg.holocore.resources.support.data.server_info.loader.GcwRegionLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.guild.GuildObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Responsibilities:
 * <ul>
 *     <li>Showing regions on the planetary map</li>
 *     <li>Keeping track of who has the most control of a region</li>
 * </ul>
 */
public class CivilWarRegionService extends Service {
	
	private static final long BASE_ZONE_POINTS = 50_000;	// Amount of points that each faction has by default in each zone. The point is to make a zone more difficult to capture if it has been reset.
	private static final String EGG_TEMPLATE = "object/tangible/spawning/shared_spawn_egg.iff";
	private static final int PRESENCE_POINTS_PER_TICK = 20;	// Base amount of GCW points contributed to a region by presence
	
	private final GcwRegionLoader regionLoader;	// Stores static information about zone names and their locations in the game
	private final PswgGcwRegionDatabase regionDatabase;	// Stores dynamic information about how many points each faction has per zone
	private final ScheduledThreadPool executor;
	private final Map<SWGObject, GcwRegionLoader.GcwRegionInfo> eggMap;
	
	private GuildObject guildObject;
	
	public CivilWarRegionService() {
		regionLoader = ServerData.INSTANCE.getGcwRegionLoader();
		regionDatabase = PswgDatabase.INSTANCE.getGcwRegions();
		executor = new ScheduledThreadPool(1, "civil-war-region-service");
		eggMap = Collections.synchronizedMap(new HashMap<>());
	}
	
	@Override
	public boolean start() {
		boolean startable = super.start() && guildObject != null;
		
		if (!startable) {
			return false;
		}
		
		Map<Terrain, Collection<GcwRegionLoader.GcwRegionInfo>> regionsByTerrain = regionLoader.getRegionsByTerrain();
		
		for (Map.Entry<Terrain, Collection<GcwRegionLoader.GcwRegionInfo>> terrainCollectionEntry : regionsByTerrain.entrySet()) {
			Terrain terrain = terrainCollectionEntry.getKey();
			Collection<GcwRegionLoader.GcwRegionInfo> terrainRegions = terrainCollectionEntry.getValue();
			
			for (GcwRegionLoader.GcwRegionInfo terrainRegion : terrainRegions) {
				String regionName = terrainRegion.getRegionName();
				
				ZoneMetadata zone = regionDatabase.getZone(regionName);
				
				if (zone == null) {
					regionDatabase.createZone(regionName, BASE_ZONE_POINTS);
					guildObject.setImperialZonePercent(null, regionName, 50);	// Start the zone as perfectly balanced. You know, as all things should be.
				} else {
					int imperialPercentage = calculateImperialPercent(zone.getImperialPoints(), zone.getRebelPoints());
					guildObject.setImperialZonePercent(null, regionName, imperialPercentage);
				}
				
				SWGObject egg = ObjectCreator.createObjectFromTemplate(EGG_TEMPLATE);
				Location location = Location.builder()
						.setTerrain(terrain)
						.setX(terrainRegion.getCenterX())
						.setY(0)	// TODO read height of terrain and use that as y-coordinate to avoid having the egg spawning inside a hill or something
						.setZ(terrainRegion.getCenterZ())
						.build();
				
				egg.moveToContainer(null, location);	// Spawn egg at desired position
				egg.setObjectName(regionName);	// Give the egg the name of the region
				ObjectCreatedIntent.broadcast(egg);
				eggMap.put(egg, terrainRegion);
			}
		}
		
		// Decay or boost percentage of zones based on player presence every 10 minutes
		long checkRate = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
		executor.start();
		executor.executeWithFixedRate(checkRate, checkRate, this::updateRegionalPresence);
		
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		return super.stop();
	}
	
	@IntentHandler
	private void handleObjectCreated(ObjectCreatedIntent intent) {
		SWGObject genericObject = intent.getObject();
		
		if (genericObject.getGameObjectType() == GameObjectType.GOT_GUILD) {
			guildObject = (GuildObject) genericObject;
		}
	}
	
	/**
	 * Handles showing GCW zones to the player.
	 */
	@IntentHandler
	private void handleInboundPacket(InboundPacketIntent intent) {
		Player player = intent.getPlayer();
		SWGPacket packet = intent.getPacket();
		
		if (!(packet instanceof GcwRegionsReq)) {
			return;
		}
		
		Map<Terrain, Collection<GcwRegionLoader.GcwRegionInfo>> regionsByTerrain = regionLoader.getRegionsByTerrain();
		Collection<GcwRegion> clientRegions = new ArrayList<>();
		Collection<GcwGroup> clientGroups = new ArrayList<>();
		
		for (Map.Entry<Terrain, Collection<GcwRegionLoader.GcwRegionInfo>> regionInfoEntry : regionsByTerrain.entrySet()) {
			Terrain terrain = regionInfoEntry.getKey();
			Collection<GcwRegionLoader.GcwRegionInfo> serverRegions = regionInfoEntry.getValue();
			
			Collection<GcwRegionZone> clientRegionZones = serverRegions.stream()
					.map(info -> new GcwRegionZone(info.getRegionName(), info.getCenterX(), info.getCenterZ(), info.getRadius()))
					.collect(Collectors.toList());
			
			Collection<GcwGroupZone> clientGroupZones = serverRegions.stream()
					.map(info -> new GcwGroupZone(info.getRegionName(), 0))
					.collect(Collectors.toList());
			
			GcwRegion clientRegion = new GcwRegion(terrain.getName(), clientRegionZones);
			clientRegions.add(clientRegion);
			
			GcwGroup clientGroup = new GcwGroup(terrain.getName(), clientGroupZones);
			clientGroups.add(clientGroup);
		}
		
		player.sendPacket(
				new GcwRegionsRsp(clientRegions),	// Show GCW regions below the category "Galactic Civil War Contested Zone" in the Locations portion of the planetary map
				new GcwGroupsRsp(clientGroups)	// Show GCW regions as Locations on the map portion of the planetary map
		);
	}
	
	/**
	 * Counts points towards the relevant faction if the points were awarded inside a region
	 */
	@IntentHandler
	private void handleCivilWarPoint(CivilWarPointIntent intent) {
		PlayerObject player = intent.getReceiver();
		CreatureObject creature = (CreatureObject) player.getParent();
		
		Objects.requireNonNull(creature, "PlayerObject without a parent");
		
		PvpFaction pvpFaction = creature.getPvpFaction();
		
		if (pvpFaction == PvpFaction.NEUTRAL) {
			// Not sure how a neutral would receive GCW points, but that shouldn't tip the scales in either direction
			return;
		}
		
		
		int points = intent.getPoints();
		Optional<GcwRegionLoader.GcwRegionInfo> regionInfoOptional = regionLoader.getRegion(creature);
		
		if (regionInfoOptional.isEmpty()) {
			// Player didn't receive GCW points inside a region
			return;
		}
		
		GcwRegionLoader.GcwRegionInfo regionInfo = regionInfoOptional.get();
		String zoneName = regionInfo.getRegionName();
		
		ZoneMetadata zoneMetadata = regionDatabase.getZone(zoneName);
		
		if (zoneMetadata == null) {
			Log.w("Synchronization issue between SDB file and database, unable to find zone %s in database", zoneName);
			return;
		}
		
		long rebelTotal = zoneMetadata.getRebelPoints();
		long imperialTotal = zoneMetadata.getImperialPoints();
		
		switch (pvpFaction) {
			case REBEL: {
				rebelTotal += points;
				regionDatabase.setRebelPoints(zoneName, rebelTotal);
				break;
			}
			case IMPERIAL: {
				imperialTotal += points;
				regionDatabase.setImperialPoints(zoneName, imperialTotal);
				break;
			}
		}
		
		int imperialPercent = calculateImperialPercent(imperialTotal, rebelTotal);
		guildObject.setImperialZonePercent(regionInfo.getTerrain(), zoneName, imperialPercent);
	}
	
	/**
	 * Checks presence of players in every GCW region.
	 * Each faction can be awarded or deducted points based on presence.
	 * Scales with amount of players present.
	 */
	private void updateRegionalPresence() {
		for (Map.Entry<SWGObject, GcwRegionLoader.GcwRegionInfo> entry : eggMap.entrySet()) {
			SWGObject egg = entry.getKey();
			GcwRegionLoader.GcwRegionInfo region = entry.getValue();
			String zoneName = region.getRegionName();
			ZoneMetadata zoneMetadata = regionDatabase.getZone(zoneName);
			
			if (zoneMetadata == null) {
				Log.w("Synchronization issue between SDB file and database, unable to find zone %s in database", zoneName);
				return;
			}
			
			Set<Player> observers = egg.getObservers();
			long rebelPoints = zoneMetadata.getRebelPoints();
			
			{
				List<CreatureObject> rebels = presentPlayers(observers, PvpFaction.REBEL);
				
				long rebelCount = rebels.size();
				
				if (rebelCount <= 0) {
					// No rebels present. Deduct points.
					rebelPoints = Math.max(zoneMetadata.getRebelPoints() - PRESENCE_POINTS_PER_TICK, BASE_ZONE_POINTS);
					
					regionDatabase.setRebelPoints(zoneName, rebelPoints);
				} else {
					// Rebels are present. Award points that scales with the amount of rebels in the area.
					long rebelRankIncrease = getRankBonusIncrease(rebels);
					rebelPoints += rebelRankIncrease / 100 * PRESENCE_POINTS_PER_TICK;
					rebelPoints += PRESENCE_POINTS_PER_TICK * rebelCount;
					
					regionDatabase.setRebelPoints(zoneName, rebelPoints);
				}
				
			}
			
			long imperialPoints = zoneMetadata.getImperialPoints();
			
			{
				List<CreatureObject> imperials = presentPlayers(observers, PvpFaction.IMPERIAL);
				
				long imperialCount = imperials.size();
				
				if (imperialCount <= 0) {
					// No imperials present. Deduct points.
					imperialPoints = Math.max(zoneMetadata.getImperialPoints() - PRESENCE_POINTS_PER_TICK, BASE_ZONE_POINTS);
					
					regionDatabase.setImperialPoints(zoneName, imperialPoints);
				} else {
					// Imperials  are present. Award points that scales with the amount of imperials in the area.
					long imperialRankIncrease = getRankBonusIncrease(imperials);
					imperialPoints += imperialRankIncrease / 100 * PRESENCE_POINTS_PER_TICK;
					imperialPoints += PRESENCE_POINTS_PER_TICK * imperialCount;
					
					regionDatabase.setImperialPoints(zoneName, imperialPoints);
				}
				
			}
			// Update zone percent on planetary map if applicable
			guildObject.setImperialZonePercent(region.getTerrain(), zoneName, calculateImperialPercent(imperialPoints, rebelPoints));
		}
	}
	
	private List<CreatureObject> presentPlayers(Set<Player> observers, PvpFaction faction) {
		return observers.stream()
				.filter(new ActivePlayerPredicate())
				.map(Player::getCreatureObject)
				.filter(creatureObject -> creatureObject
				.getPvpStatus() == PvpStatus.SPECIALFORCES)    // Only Special Forces players can prevent decay
				.filter(creatureObject -> creatureObject.getPvpFaction() == faction)
				.collect(Collectors.toList());
	}
	
	private long getRankBonusIncrease(Collection<CreatureObject> players) {
		return players.stream()
				.map(CreatureObject::getPlayerObject)
				.mapToInt(PlayerObject::getCurrentGcwRank)
				.map(i -> i +1)	// Rank is zero-indexed
				.sum() * 10;
	}
	
	private int calculateImperialPercent(long imperialTotal, long rebelTotal) {
		long sum = imperialTotal + rebelTotal;
		
		if (sum <= 0) {
			return 50;	// Return to 50/50 in case things are about to go horribly wrong
		}
		
		return (int) ((double) imperialTotal / (double) (imperialTotal + rebelTotal) * 100);
	}
	
}
