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
package com.projectswg.holocore.resources.gameplay.world.travel;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.info.Config;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.world.travel.TravelGroup.ShuttleStatus;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.SpecificObject;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TravelHelper {
	
	private final Map<String, TravelGroup> travel;
	private final ThreadPool travelExecutor;
	private final AllowedRouteManager routeManager;
	private final TravelPointManager pointManager;
	
	public TravelHelper() {
		this.travel = new ConcurrentHashMap<>();
		this.travelExecutor = new ThreadPool(3, "travel-shuttles-%d");
		this.routeManager = new AllowedRouteManager();
		this.pointManager = new TravelPointManager();
		
		loadAllowedRoutesAndPrices();
		createGalaxyTravels();
		loadTravelPoints();
	}
	
	public void start() {
		travelExecutor.start();
		for (TravelGroup gt : travel.values())
			travelExecutor.execute(gt);
	}
	
	public void stop() {
		travelExecutor.stop(true);
	}
	
	public void addTravelPoint(TravelPoint point) {
		pointManager.addTravelPoint(point);
	}
	
	public boolean isValidRoute(Terrain departure, Terrain destination) {
		return routeManager.isRouteAvailable(departure, destination);
	}
	
	public int getTravelFee(Terrain departure, Terrain destination) {
		return routeManager.getRouteFee(departure, destination);
	}
	
	public TravelGroup getTravelGroup(String template) {
		return travel.get(template);
	}
	
	public List<TravelPoint> getAvailableTravelPoints(SWGObject traveller, Terrain terrain) {
		if (!isValidRoute(traveller.getTerrain(), terrain))
			return new ArrayList<>();
		TravelPoint nearest = getNearestTravelPoint(traveller);
		return pointManager.getPointsForTerrain(nearest, terrain);
	}
	
	public TravelPoint getDestinationPoint(SWGObject ticket) {
		Terrain planet = Terrain.getTerrainFromName(ticket.getAttribute("@obj_attr_n:travel_arrival_planet").split(":")[1]);
		String point = ticket.getAttribute("@obj_attr_n:travel_arrival_point");
		return getDestinationPoint(planet, point);
	}
	
	public TravelPoint getDestinationPoint(Terrain terrain, String pointName) {
		return pointManager.getDestination(terrain, pointName);
	}
	
	public TravelPoint getNearestTravelPoint(SWGObject object) {
		return pointManager.getNearestPoint(object.getWorldLocation());
	}
	
	public void grantTicket(TravelPoint departure, TravelPoint destination, SWGObject receiver) {
		// Create the ticket object
		SWGObject ticket = SpecificObject.SO_TRAVEL_TICKET.createType();
		Log.t("Granting ticket for departure: %s/%s  and destination: %s/%s", departure.getLocation(), departure.getName(), destination.getLocation(), destination.getName());
		
		// Departure attributes
		ticket.addAttribute("@obj_attr_n:travel_departure_planet", "@planet_n:" + departure.getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_departure_point", departure.getName());
		
		// Arrival attributes
		ticket.addAttribute("@obj_attr_n:travel_arrival_planet", "@planet_n:" + destination.getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_arrival_point", destination.getName());
		
		ticket.moveToContainer(receiver.getSlottedObject("inventory"));
		new ObjectCreatedIntent(ticket).broadcast();
	}
	
	public List<SWGObject> getTickets(CreatureObject creature) {
		Collection<SWGObject> tickets = creature.getItemsByTemplate("inventory", SpecificObject.SO_TRAVEL_TICKET.getTemplate());
		List<SWGObject> usableTickets = new ArrayList<>();
		
		for (SWGObject ticket : tickets) {
			if (isTicket(ticket) && isTicketUsable(ticket, getNearestTravelPoint(ticket)))
				usableTickets.add(ticket);
		}
		return usableTickets;
	}
	
	public boolean isTicket(SWGObject object) {
		String departurePlanet = object.getAttribute("@obj_attr_n:travel_departure_planet");
		String departureDestination = object.getAttribute("@obj_attr_n:travel_departure_point");
		String arrivalPlanet = object.getAttribute("@obj_attr_n:travel_arrival_planet");
		String arrivalPoint = object.getAttribute("@obj_attr_n:travel_arrival_point");
		
		return departurePlanet != null && departureDestination != null && arrivalPlanet != null && arrivalPoint != null;
	}
	
	public boolean isTicketUsable(SWGObject ticket, TravelPoint nearest) {
		String departurePoint = ticket.getAttribute("@obj_attr_n:travel_departure_point");
		String departurePlanet = ticket.getAttribute("@obj_attr_n:travel_departure_planet");
		Terrain departureTerrain = Terrain.getTerrainFromName(departurePlanet.split(":")[1]);
		Terrain currentTerrain = ticket.getTerrain();
		
		return departureTerrain == currentTerrain && departurePoint.equals(nearest.getName());
	}
	
	public void handleTicketUse(Player player, SWGObject ticket, TravelPoint nearestPoint, TravelPoint destinationPoint) {
		CreatureObject traveler = player.getCreatureObject();
		if (!isTicket(ticket)) {
			Log.e("%s attempted to use an object that isn't a ticket!", player);
		} else if (nearestPoint.getGroup().getStatus() != ShuttleStatus.GROUNDED) {
			int time = nearestPoint.getGroup().getTimeRemaining();
			new SystemMessageIntent(player, new ProsePackage(new StringId("travel/travel", "shuttle_board_delay"), "DI", time)).broadcast();
		} else if (!isTicketUsable(ticket, nearestPoint)) {
			// This ticket isn't valid for this point
			new SystemMessageIntent(player, "@travel:wrong_shuttle").broadcast();
		} else if (nearestPoint.isWithinRange(player.getCreatureObject())) {
			// They can use their ticket if they're within range.
			Log.i("%s/%s is traveling from %s to %s", player.getUsername(), traveler.getObjectName(), nearestPoint.getName(), destinationPoint);
			teleportAndDestroyTicket(destinationPoint, ticket, traveler);
		} else {
			new SystemMessageIntent(player, "@travel:boarding_too_far").broadcast();
		}
	}
	
	private void teleportAndDestroyTicket(TravelPoint destination, SWGObject ticket, CreatureObject traveler) {
		DestroyObjectIntent.broadcast(ticket);
		traveler.moveToContainer(destination.getCollector().getParent(), destination.getLocation());
	}
	
	private void loadAllowedRoutesAndPrices() {
		DatatableData travelFeeTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/travel/travel.iff");
		// Sets up the travelPlanets array to be in the order of the datatable
		Terrain [] travelPlanets = new Terrain[travelFeeTable.getRowCount()];
		travelFeeTable.handleRows(currentRow -> travelPlanets[currentRow] = Terrain.getTerrainFromName((String) travelFeeTable.getCell(currentRow, "Planet")));
		
		for (int rowIndex = 0; rowIndex < travelPlanets.length; rowIndex++) {
			for (int columnIndex = rowIndex; columnIndex < travelPlanets.length; columnIndex++) {
				int price = (int) travelFeeTable.getCell(rowIndex, columnIndex+1);
				
				if (price <= 0)	// If price is below or equal to 0 then this is an invalid route and isn't an option.
					continue;
				
				routeManager.addRoute(travelPlanets[rowIndex], travelPlanets[columnIndex], price);
				routeManager.addRoute(travelPlanets[columnIndex], travelPlanets[rowIndex], price);
			}
		}
	}
	
	private void createGalaxyTravels() {
		Config config = DataManager.getConfig(ConfigFile.FEATURES);
		long groundTime = config.getInt("SHUTTLE-GROUND-TIME", 120);
		long airTime = config.getInt("SHUTTLE-AIR-TIME", 60);
		
		createGalaxyTravel(SpecificObject.SO_TRANSPORT_SHUTTLE.getTemplate(), 17, groundTime, airTime);
		createGalaxyTravel(SpecificObject.SO_TRANSPORT_STARPORT.getTemplate(), 21, groundTime, airTime);
		createGalaxyTravel(SpecificObject.SO_TRANSPORT_STARPORT_THEED.getTemplate(), 24, groundTime, airTime);
	}
	
	private void createGalaxyTravel(String template, long landTime, long groundTime, long airTime) {
		travel.put(template, new TravelGroup(landTime * 1000, groundTime * 1000, airTime * 1000));
	}
	
	private void loadTravelPoints() {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/travel/travel.sdb"))) {
			while (set.next()) {
				loadTravelPoint(set);
			}
		} catch (IOException e) {
			Log.e("Failed to load a travel point");
			Log.e(e);
		}
	}
	
	private void loadTravelPoint(SdbResultSet set) {
		String pointName = set.getText("name");
		double x = set.getReal("x");
		double z = set.getReal("z");
		String type = set.getText("type");
		Terrain travelPlanet = Terrain.getTerrainFromName(set.getText("planet"));
		if (travelPlanet == null) {
			Log.e("Invalid planet in travel.sdb: %s", set.getText("planet"));
			return;
		}
		
		TravelGroup group = getTravelGroupForType(type);
		TravelPoint point = new TravelPoint(pointName, new Location(x, 0, z, travelPlanet), group, type.endsWith("starport"));
		group.addTravelPoint(point);
		pointManager.addTravelPoint(point);
	}
	
	private TravelGroup getTravelGroupForType(String type) {
		switch (type) {
			case "shuttleport":
				return getTravelGroup(SpecificObject.SO_TRANSPORT_SHUTTLE.getTemplate());
			case "starport":
				return getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT.getTemplate());
			case "theed_starport":
				return getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT_THEED.getTemplate());
			default:
				Log.w("Invalid travel point type: %s", type);
				return null;
		}
	}
	
	private static class AllowedRouteManager {
		
		private final Map<Terrain, Map<Terrain, Integer>> routeCosts;
		
		public AllowedRouteManager() {
			this.routeCosts = new ConcurrentHashMap<>();
		}
		
		public void addRoute(Terrain departure, Terrain destination, int fee) {
			Map<Terrain, Integer> departureCosts = routeCosts.computeIfAbsent(departure, k -> new ConcurrentHashMap<>());
			departureCosts.put(destination, fee);
		}
		
		public boolean isRouteAvailable(Terrain departure, Terrain destination) {
			return routeCosts.get(departure) != null && routeCosts.get(departure).get(destination) != null;
		}
		
		public int getRouteFee(Terrain departure, Terrain destination) {
			Integer fee = routeCosts.get(departure).get(destination);
			if (fee == null)
				return 0;
			return fee;
		}
	}
	
}
