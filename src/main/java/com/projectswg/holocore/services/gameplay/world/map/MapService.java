package com.projectswg.holocore.services.gameplay.world.map;

import com.projectswg.common.data.encodables.map.MapLocation;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.spatial.GetMapLocationsMessage;
import com.projectswg.common.network.packets.swg.zone.spatial.GetMapLocationsResponseMessage;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.world.map.MapCategory;
import com.projectswg.holocore.resources.gameplay.world.map.MappingTemplate;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MapService extends Service {
	
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
	
	public MapService() {
		mapCategories = new HashMap<>();
		mappingTemplates = new HashMap<>();

		staticMapLocations = new ConcurrentHashMap<>();
		dynamicMapLocations = new ConcurrentHashMap<>();
		persistentMapLocations = new ConcurrentHashMap<>();
		// Needs to be done here as ObjectManager is initialized before MapService otherwise there won't be any map locations
		// for objects loaded from the databases, snapshots, buildouts.
		loadMapCategories();
		loadMappingTemplates();
	}

	@Override
	public boolean initialize() {
		loadStaticCityPoints();
		return true;
	}

	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		switch (packet.getPacketType()) {
			case GET_MAP_LOCATIONS_MESSAGE:
				if (packet instanceof GetMapLocationsMessage)
					handleMapLocationsRequest(gpi.getPlayer(), (GetMapLocationsMessage) packet);
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		addMapLocation(oci.getObject(), MapType.STATIC);
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

	private void addMapLocation(SWGObject object, MapType type) {
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
		} else {
			location.setId(1);
			staticMapLocations.put(planet, new ArrayList<>());
		}
		staticMapLocations.get(planet).add(location);
	}

	public void addDynamicMapLocation(String planet, MapLocation location) {
		if (dynamicMapLocations.containsKey(planet)) {
			location.setId(dynamicMapLocations.get(planet).size() + 1);
		} else {
			location.setId(1);
			dynamicMapLocations.put(planet, new ArrayList<>());
		}
		dynamicMapLocations.get(planet).add(location);
	}

	public void addPersistentMapLocation(String planet, MapLocation location) {
		if (persistentMapLocations.containsKey(planet)) {
			location.setId(persistentMapLocations.get(planet).size() + 1);
		} else {
			location.setId(1);
			persistentMapLocations.put(planet, new ArrayList<>());
		}
		persistentMapLocations.get(planet).add(location);
	}

	public enum MapType {
		STATIC,
		DYNAMIC,
		PERSISTENT
	}
	
}
