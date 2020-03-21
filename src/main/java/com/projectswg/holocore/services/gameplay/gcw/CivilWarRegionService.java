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
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
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
import com.projectswg.holocore.resources.support.data.server_info.loader.GcwRegionLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgGcwRegionDatabase;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.ZoneMetadata;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
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
import java.util.stream.Collectors;

/**
 * Responsibilities:
 * <ul>
 *     <li>Showing regions on the planetary map</li>
 *     <li>Keeping track of who has the most control of a region</li>
 * </ul>
 */
public class CivilWarRegionService extends Service {
	
	private static final String EGG_TEMPLATE = "object/tangible/spawning/shared_spawn_egg.iff";
	
	private final GcwRegionLoader regionLoader;	// Stores static information about zone names and their locations in the game
	private final PswgGcwRegionDatabase regionDatabase;	// Stores dynamic information about how many points each faction has per zone
	private final ScheduledThreadPool executor;
	private final Collection<SWGObject> eggs;
	
	private GuildObject guildObject;
	
	public CivilWarRegionService() {
		regionLoader = ServerData.INSTANCE.getGcwRegionLoader();
		regionDatabase = PswgDatabase.INSTANCE.getGcwRegions();
		executor = new ScheduledThreadPool(1, "civil-war-region-service");
		eggs = Collections.synchronizedCollection(new ArrayList<>());
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
					regionDatabase.createZone(regionName);
					guildObject.setImperialZonePercent(null, regionName, 50);	// Start the zone as perfectly balanced. You know, as all things should be.
				} else {
					int imperialPercentage = calculateImperialPercent(zone.getImperialPoints(), zone.getRebelPoints());
					guildObject.setImperialZonePercent(null, regionName, imperialPercentage);
				}
				
				SWGObject egg = ObjectCreator.createObjectFromTemplate(EGG_TEMPLATE);
				Location location = Location.builder()
						.setTerrain(terrain)
						.setX(terrainRegion.getCenterX())
						.setY(0)	// TODO read height of terrain and use that as y-coordinate
						.setZ(terrainRegion.getCenterZ())
						.build();
				
				egg.moveToContainer(null, location);	// Spawn egg at desired position
				ObjectCreatedIntent.broadcast(egg);
				eggs.add(egg);
			}
		}
		
		long checkRate = 1000;	// Attempt to delete old NPCs every 1000ms
		executor.start();
		executor.executeWithFixedRate(checkRate, checkRate, this::grantPresencePoints);
		
		return true;
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
		PvpFaction pvpFaction = creature.getPvpFaction();
		
		Objects.requireNonNull(creature, "PlayerObject without a parent");
		
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
			Log.w("Synchronization issue between SDB file and database, unable to find zone %s", zoneName);
			return;
		}
		
		long rebelTotal = zoneMetadata.getRebelPoints();
		long imperialTotal = zoneMetadata.getImperialPoints();
		
		switch (pvpFaction) {
			case REBEL: {
				rebelTotal += points;
				regionDatabase.addRebelPoints(zoneName, rebelTotal);
				break;
			}
			case IMPERIAL: {
				imperialTotal += points;
				regionDatabase.addImperialPoints(zoneName, imperialTotal);
				break;
			}
		}
		
		int imperialPercent = calculateImperialPercent(imperialTotal, rebelTotal);
		guildObject.setImperialZonePercent(regionInfo.getTerrain(), zoneName, imperialPercent);
	}
	
	private void grantPresencePoints() {
		for (SWGObject egg : eggs) {
			Set<Player> observers = egg.getObservers();
			
			for (Player observer : observers) {
				CreatureObject creatureObject = observer.getCreatureObject();
				PvpFaction pvpFaction = creatureObject.getPvpFaction();
				
				boolean incapacitated = creatureObject.getPosture() == Posture.INCAPACITATED;
				boolean dead = creatureObject.getPosture() == Posture.DEAD;
				boolean cloaked = !creatureObject.isVisible();
				boolean notSpecialForces = !creatureObject.hasPvpFlag(PvpFlag.OVERT);
				boolean afk = creatureObject.getPlayerObject().isFlagSet(PlayerFlags.AFK);
				boolean offline = creatureObject.getPlayerObject().isFlagSet(PlayerFlags.LD);
				
				if (incapacitated || dead || cloaked || notSpecialForces || afk || offline) {
					// Player must participate in GCW. Being present is not enough.
					return;
				}
				
				// TODO how do we know that they've been in the area an appropriate amount of time?
			}
		}
	}
	
	private void decayPresencePoints() {
		for (SWGObject egg : eggs) {
			Set<Player> observers = egg.getObservers();
			
			long rebels = observers.stream()
					.map(Player::getCreatureObject)
					.filter(creatureObject -> creatureObject.hasPvpFlag(PvpFlag.OVERT))	// Only Special Forces players can prevent decay
					.filter(creatureObject -> {
						PlayerObject playerObject = creatureObject.getPlayerObject();
						boolean afk = playerObject.isFlagSet(PlayerFlags.AFK);
						boolean offline = playerObject.isFlagSet(PlayerFlags.LD);
						boolean incapacitated = creatureObject.getPosture() == Posture.INCAPACITATED;
						boolean dead = creatureObject.getPosture() == Posture.DEAD;
						boolean cloaked = !creatureObject.isVisible();
						boolean specialForces = creatureObject.hasPvpFlag(PvpFlag.OVERT);	// TODO separate filter
						
						return !afk && !offline && !incapacitated && !dead && !cloaked && specialForces;
					})
					.map(CreatureObject::getPvpFaction)
					.filter(faction -> faction == PvpFaction.REBEL)
					.count();

			if (rebels <= 0) {
				// No rebels present. Deduct points.
				
			}
			
			long imperials = observers.stream()
					.map(Player::getCreatureObject)
					.map(CreatureObject::getPvpFaction)
					.filter(faction -> faction == PvpFaction.IMPERIAL)
					.count();
			
			if (imperials <= 0) {
				// No imperials present. Deduct points.
				
			}
		}
	}
	
	private int calculateImperialPercent(long imperialTotal, long rebelTotal) {
		long sum = imperialTotal + rebelTotal;
		
		if (sum <= 0) {
			return 50;	// Return to 50/50 in case things are about to go horribly wrong
		}
		
		return (int) ((double) imperialTotal / (double) (imperialTotal + rebelTotal) * 100);
	}
	
	// TODO scheduled job that deducts points from factions that are not present in all zones, until the base amount of 50_000 is reached
	
	// TODO scheduled job that checks legitimate factional presence inside each zone. This will count towards points in that zone for the relevant faction.
}
