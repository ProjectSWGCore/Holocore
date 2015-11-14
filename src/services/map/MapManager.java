/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 * <p/>
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 * <p/>
 * This file is part of Holocore.
 * <p/>
 * --------------------------------------------------------------------------------
 * <p/>
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p/>
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package services.map;

import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreatedIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.spatial.GetMapLocationsMessage;
import network.packets.swg.zone.spatial.GetMapLocationsResponseMessage;
import resources.client_info.ClientFactory;
import resources.client_info.ServerFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MapManager extends Manager {
	
	private final Map<String, MapCategory> mapCategories;
	private final Map<String, MappingTemplate> mappingTemplates;

	private final Map<String, ArrayList<MapLocation>> staticMapLocations; // ex: NPC cities and buildings
	private final Map<String, ArrayList<MapLocation>> dynamicMapLocations; // ex: camps, faction presences (grids)
	private final Map<String, ArrayList<MapLocation>> persistentMapLocations; // ex: Player structures, vendors

	// Version is used to determine when the client needs to be updated. 0 sent by client if no map requested yet.
	// AtomicInteger used for synchronization
	private final AtomicInteger staticMapVersion = new AtomicInteger(1);
	private final AtomicInteger dynamicMapVersion = new AtomicInteger(1);
	private final AtomicInteger persistMapVersion = new AtomicInteger(1);
	
	private final CityService cityService;

	public MapManager() {
		cityService = new CityService();
		mapCategories = new HashMap<>();
		mappingTemplates = new HashMap<>();

		staticMapLocations = new ConcurrentHashMap<>();
		dynamicMapLocations = new ConcurrentHashMap<>();
		persistentMapLocations = new ConcurrentHashMap<>();
		// Needs to be done here as ObjectManager is initialized before MapService otherwise there won't be any map locations
		// for objects loaded from the databases, snapshots, buildouts.
		loadMapCategories();
		loadMappingTemplates();
		
		addChildService(cityService);
		
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		loadStaticCityPoints();
		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processPacket((GalacticPacketIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					addMapLocation(((ObjectCreatedIntent) i).getObject(), MapType.STATIC);
				break;
			default:
				break;
		}
	}

	private void processPacket(GalacticPacketIntent intent) {
		Player player = intent.getPlayerManager().getPlayerFromNetworkId(intent.getNetworkId());
		if (player == null)
			return;
		Packet p = intent.getPacket();
		if (p instanceof SWGPacket)
			processSwgPacket(player, (SWGPacket) p);
	}
	
	private void processSwgPacket(Player player, SWGPacket p) {
		switch (p.getPacketType()) {
			case GET_MAP_LOCATIONS_MESSAGE:
				if (p instanceof GetMapLocationsMessage)
					handleMapLocationsRequest(player, (GetMapLocationsMessage) p);
				break;
			default:
				break;
		}
	}

	private void handleMapLocationsRequest(Player player, GetMapLocationsMessage p) {
		String planet = p.getPlanet();

		int staticVer = staticMapVersion.get();
		int dynamicVer = dynamicMapVersion.get();
		int persistVer = persistMapVersion.get();

		// Only send list if the current map version isn't the same as the clients.
		List<MapLocation> staticLocs = (p.getVersionStatic() != staticVer ? staticMapLocations.get(planet) : null);
		List<MapLocation> dynamicLocs = (p.getVersionDynamic() != dynamicVer ? dynamicMapLocations.get(planet) : null);
		List<MapLocation> persistLocs = (p.getVersionPersist() != persistVer ? persistentMapLocations.get(planet) : null);

		GetMapLocationsResponseMessage responseMessage = new GetMapLocationsResponseMessage(planet,
				staticLocs, dynamicLocs, persistLocs, staticVer, dynamicVer, persistVer);

		player.sendPacket(responseMessage);
	}

	private void loadMapCategories() {
		DatatableData table = (DatatableData) ClientFactory.getInfoFromFile("datatables/player/planet_map_cat.iff");
		for (int row = 0; row < table.getRowCount(); row++) {
			MapCategory category = new MapCategory();
			category.setName(table.getCell(row, 0).toString());
			category.setIndex((Integer) table.getCell(row, 1));
			category.setIsCategory(Boolean.valueOf(table.getCell(row, 2).toString()));
			category.setIsSubCategory(Boolean.valueOf(table.getCell(row, 3).toString()));
			category.setCanBeActive(Boolean.valueOf(table.getCell(row, 4).toString()));
			category.setFaction(table.getCell(row, 5).toString());
			category.setFactionVisibleOnly(Boolean.valueOf(table.getCell(row, 6).toString()));
			mapCategories.put(category.getName(), category);
		}
	}

	private void loadMappingTemplates() {
		DatatableData table = ServerFactory.getDatatable("map/map_locations.iff");
		for (int row = 0; row < table.getRowCount(); row++) {
			MappingTemplate template = new MappingTemplate();
			template.setTemplate(ClientFactory.formatToSharedFile(table.getCell(row, 0).toString()));
			template.setName(table.getCell(row, 1).toString());
			template.setCategory(table.getCell(row, 2).toString());
			template.setSubcategory(table.getCell(row, 3).toString());
			template.setType((Integer) table.getCell(row, 4));
			template.setFlag((Integer) table.getCell(row, 5));

			mappingTemplates.put(template.getTemplate(), template);
		}
	}

	private void loadStaticCityPoints() {
		DatatableData table = ServerFactory.getDatatable("map/static_city_points.iff");

		byte city = (byte) mapCategories.get("city").getIndex();
		for (int row = 0; row < table.getRowCount(); row++) {
			ArrayList<MapLocation> locations = staticMapLocations.get(table.getCell(row, 0).toString());
			if (locations == null) {
				locations = new ArrayList<>();
				staticMapLocations.put((String) table.getCell(row, 0), locations);
			}

			String name = (String) table.getCell(row, 1);
			float x = (float) table.getCell(row, 2);
			float z = (float) table.getCell(row, 3);
			locations.add(new MapLocation(locations.size() + 1, name, x, z, city, (byte) 0, false));
		}
	}

	public void addMapLocation(SWGObject object, MapType type) {
		if (!mappingTemplates.containsKey(object.getTemplate()))
			return;

		MappingTemplate mappingTemplate = mappingTemplates.get(object.getTemplate());

		MapLocation mapLocation = new MapLocation();
		mapLocation.setName(mappingTemplate.getName());
		mapLocation.setCategory((byte) mapCategories.get(mappingTemplate.getCategory()).getIndex());
		if (mappingTemplate.getSubcategory() != null && !mappingTemplate.getSubcategory().isEmpty())
			mapLocation.setSubcategory((byte) mapCategories.get(mappingTemplate.getSubcategory()).getIndex());
		else
			mapLocation.setSubcategory((byte) 0);
		mapLocation.setX((float) object.getX());
		mapLocation.setY((float) object.getZ());

		String planet = object.getTerrain().getName();

		switch (type) {
			case STATIC:
				addStaticMapLocation(planet, mapLocation);
				break;
			case DYNAMIC:
				addDynamicMapLocation(planet, mapLocation);
				break;
			case PERSISTENT:
				addPersistentMapLocation(planet, mapLocation);
				break;
		}
	}

	public void addStaticMapLocation(String planet, MapLocation location) {
		if (staticMapLocations.containsKey(planet)) {
			location.setId(staticMapLocations.get(planet).size() + 1);
			staticMapLocations.get(planet).add(location);
		} else {
			location.setId(1);
			staticMapLocations.put(planet, new ArrayList<>());
			staticMapLocations.get(planet).add(location);
		}
	}

	public void addDynamicMapLocation(String planet, MapLocation location) {
		if (dynamicMapLocations.containsKey(planet)) {
			location.setId(dynamicMapLocations.get(planet).size() + 1);
			dynamicMapLocations.get(planet).add(location);
		} else {
			location.setId(1);
			dynamicMapLocations.put(planet, new ArrayList<>());
			dynamicMapLocations.get(planet).add(location);
		}
	}

	public void addPersistentMapLocation(String planet, MapLocation location) {
		if (persistentMapLocations.containsKey(planet)) {
			location.setId(persistentMapLocations.get(planet).size() + 1);
			persistentMapLocations.get(planet).add(location);
		} else {
			location.setId(1);
			persistentMapLocations.put(planet, new ArrayList<>());
			persistentMapLocations.get(planet).add(location);
		}
	}

	public enum MapType {
		STATIC,
		DYNAMIC,
		PERSISTENT
	}
}
