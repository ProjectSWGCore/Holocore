/***********************************************************************************
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
package services.galaxy.travel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import network.packets.Packet;
import network.packets.swg.zone.EnterTicketPurchaseModeMessage;
import network.packets.swg.zone.PlanetTravelPointListRequest;
import network.packets.swg.zone.PlanetTravelPointListResponse;
import intents.chat.ChatBroadcastIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.travel.*;
import resources.Location;
import resources.Posture;
import resources.Terrain;
import resources.TravelPoint;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.staticobject.StaticObject;
import resources.objects.tangible.OptionFlag;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import resources.sui.SuiButtons;
import resources.sui.SuiListBox;
import resources.sui.SuiMessageBox;
import services.galaxy.travel.TravelGroup;
import services.galaxy.travel.TravelGroup.ShuttleStatus;
import services.objects.ObjectManager;
import utilities.ThreadUtilities;

public class TravelService extends Service {
	
	private static final String DB_TABLE_NAME = "travel";
	private static final byte PLANET_NAMES_COLUMN_INDEX = 0;
	private static final short TICKET_USE_RADIUS = 8;	// The distance a player needs to be within in order to use their ticket
	
	private final ObjectManager objectManager;
	private Terrain[] travelPlanets;
	private final Map<Terrain, Map<Terrain, Integer>> allowedRoutes; // Describes which planets are linked and base prices.
	private final DatatableData travelFeeTable;
	
	private final AtomicInteger groundTime;
	private final AtomicInteger airTime;
	private final double ticketPriceFactor;
	
	// Fields relating to shuttle take-off and landing
	private final Map<String, TravelGroup> travel;
	private ExecutorService executor;
	
	public TravelService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		
		allowedRoutes = new HashMap<>();
		travelFeeTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/travel/travel.iff");
		travel = new HashMap<>();
		
		ticketPriceFactor = getConfig(ConfigFile.FEATURES).getDouble("TICKET-PRICE-FACTOR", 1);
		groundTime = new AtomicInteger(getConfig(ConfigFile.FEATURES).getInt("SHUTTLE-GROUND-TIME", 120));
		airTime = new AtomicInteger(getConfig(ConfigFile.FEATURES).getInt("SHUTTLE-AIR-TIME", 60));
		
		createGalaxyTravel("object/creature/npc/theme_park/shared_player_shuttle.iff", 17000);
		createGalaxyTravel("object/creature/npc/theme_park/shared_player_transport.iff", 21000);
		createGalaxyTravel("object/creature/npc/theme_park/shared_player_transport_theed_hangar.iff", 24000);
		loadTravelPlanetNames();
		loadAllowedRoutesAndPrices();
		loadTravelPoints();
		
		registerForIntent(TravelPointSelectionIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(TicketPurchaseIntent.TYPE);
		registerForIntent(TicketUseIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
	}
	
	@Override
	public boolean start() {
		executor = Executors.newFixedThreadPool(travel.size(), ThreadUtilities.newThreadFactory("travel-shuttles-%d"));
		for (TravelGroup gt : travel.values())
			executor.execute(gt);
		
		return super.start();
	}
	
	@Override
	public boolean stop() {
		executor.shutdownNow();
		
		return super.stop();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case TravelPointSelectionIntent.TYPE:
				if (i instanceof TravelPointSelectionIntent)
					handlePointSelection((TravelPointSelectionIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					handleTravelPointRequest((GalacticPacketIntent) i);
				break;
			case TicketPurchaseIntent.TYPE:
				if (i instanceof TicketPurchaseIntent)
					handleTicketPurchase((TicketPurchaseIntent) i);
				break;
			case TicketUseIntent.TYPE:
				if (i instanceof TicketUseIntent)
					handleTicketUse((TicketUseIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					handleObjectCreation((ObjectCreatedIntent) i);
				break;
		}
	}
	
	private void createGalaxyTravel(String template, long landTime) {
		travel.put(template, new TravelGroup(template, landTime, groundTime.get() * 1000L, airTime.get() * 1000L));
	}
	
	private void loadTravelPlanetNames() {
		travelPlanets = new Terrain[travelFeeTable.getRowCount()];
		
		travelFeeTable.handleRows(currentRow -> travelPlanets[currentRow] = Terrain.getTerrainFromName((String) travelFeeTable.getCell(currentRow, PLANET_NAMES_COLUMN_INDEX)));
	}
	
	private void loadAllowedRoutesAndPrices() {
		for(Terrain travelPlanet : travelPlanets) {
			int rowIndex = travelFeeTable.getColumnFromName(travelPlanet.getName()) - 1;
			Map<Terrain, Integer> prices = new HashMap<>();
			
			for(int columnIndex = 1; columnIndex < travelPlanets.length; columnIndex++) {
				int price = (int) travelFeeTable.getCell(rowIndex, columnIndex);
				
				if(price <= 0)	// If price is below or equal to 0 then this is an invalid route and isn't an option.
					continue;
				
				Terrain arrivalPlanet = travelPlanets[columnIndex - 1];
				Map<Terrain, Integer> reverseRoute = allowedRoutes.get(arrivalPlanet);
				
				if(reverseRoute != null) {
					Integer reverseRoutePriceObj = reverseRoute.get(travelPlanet);
					
					if(reverseRoutePriceObj != null) {
						int reverseRoutePrice = reverseRoutePriceObj.intValue();
						
						if(reverseRoutePrice > price) {
							price = reverseRoutePrice;
						}
					}
				}
				
				prices.put(arrivalPlanet, price);
			}
			
			allowedRoutes.put(travelPlanet, prices);
		}
	}
	
	/**
	 * Travel points are loaded from /serverdata/static/travel.sdb
	 * A travel point represents a travel destination.
	 * @return true if all points were loaded succesfully and false if not.
	 */
	private boolean loadTravelPoints() {
		boolean success = true;
		
		for (Terrain travelPlanet : allowedRoutes.keySet()) {
			String planetName = travelPlanet.getName();
			
			try (RelationalServerData data = RelationalServerFactory.getServerData("static/travel.db", DB_TABLE_NAME)) {
				try (ResultSet set = data.selectFromTable(DB_TABLE_NAME, null, "planet = ?", planetName)) {
					while (set.next()) {
						loadTravelPoint(set, travelPlanet);
					}
				} catch (SQLException e) {
					Log.e("TravelService", String.format("Failed to load a travel point for %s. %s", planetName, e.getLocalizedMessage()));
					e.printStackTrace();
					success = false;
				}
			}
		}
		
		return success;
	}
	
	private void loadTravelPoint(ResultSet set, Terrain travelPlanet) throws SQLException {
		String pointName = set.getString("name");
		double x = set.getDouble("x");
		double y = set.getDouble("y");
		double z = set.getDouble("z");
		String type = set.getString("type");
		
		TravelPoint point = new TravelPoint(pointName, new Location(x, y, z, travelPlanet), type.endsWith("starport"), true);
		switch (type) {
			case "shuttleport":
				travel.get("object/creature/npc/theme_park/shared_player_shuttle.iff").addTravelPoint(point);
				break;
			case "starport":
				travel.get("object/creature/npc/theme_park/shared_player_transport.iff").addTravelPoint(point);
				break;
			case "theed_starport":
				travel.get("object/creature/npc/theme_park/shared_player_transport_theed_hangar.iff").addTravelPoint(point);
				break;
		}
	}
	
	private Collection<Integer> getAdditionalCosts(Location objectLocation, Collection<TravelPoint> points) {
		Collection<Integer> additionalCosts = new ArrayList<>();
		TravelPoint nearest = getNearestTravelPoint(objectLocation);
		
		for(TravelPoint point : points) {
			additionalCosts.add(getAdditionalCost(nearest.getLocation(), point.getLocation()));
		}
		
		return additionalCosts;
	}
	
	private int getAdditionalCost(Location departureLocation, Location destinationLocation) {
		int additionalCost;
		
		// Any factor below or equal to 0 makes the ticket free.
		if(ticketPriceFactor <= 0) {
			Map<Terrain, Integer> priceMap = allowedRoutes.get(departureLocation.getTerrain());
			
			if(priceMap == null)
				priceMap = allowedRoutes.get(destinationLocation.getTerrain());
			
			Integer basePrice = priceMap.get(destinationLocation.getTerrain());
			
			if(basePrice == null)
				basePrice = priceMap.get(departureLocation.getTerrain());
			
			 basePrice *= -1;
			 
			 additionalCost = basePrice.intValue();
		} else {
			// TODO implement algorithm for the extra ticket cost.
			// TODO research the above
			additionalCost = 10;	// For now, it's just 10 credits all the time.
		}
		
		return additionalCost;
	}
	
	private void handlePointSelection(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), getNearestTravelPoint(traveler.getWorldLocation()).getName(), tpsi.isInstant()));
	}
	
	private void handleTravelPointRequest(GalacticPacketIntent i) {
		Packet p = i.getPacket();
		
		if (p instanceof PlanetTravelPointListRequest) {
			PlanetTravelPointListRequest req = (PlanetTravelPointListRequest) p;
			Player player = i.getPlayerManager().getPlayerFromNetworkId(i.getNetworkId());
			Location objectLocation = player.getCreatureObject().getWorldLocation();
			TravelPoint nearest = getNearestTravelPoint(objectLocation);
			List<TravelPoint> pointsForPlanet = new ArrayList<>();
			Terrain to = Terrain.getTerrainFromName(req.getPlanetName());
			if (to == null) {
				Log.e(this, "Unknown terrain in PlanetTravelPointListRequest: %s", req.getPlanetName());
				return;
			}
			for (TravelGroup gt : travel.values())
				gt.getPointsForTerrain(pointsForPlanet, nearest, to);
			Collections.sort(pointsForPlanet);
			
			player.sendPacket(new PlanetTravelPointListResponse(req.getPlanetName(), pointsForPlanet, getAdditionalCosts(objectLocation, pointsForPlanet)));
		}
	}
	
	private int getTicketBasePrice(Terrain departurePlanet, Terrain arrivalPlanet) {
		return allowedRoutes.get(departurePlanet).get(arrivalPlanet);
	}
	
	private int getTotalTicketPrice(TravelPoint departurePoint, TravelPoint arrivalPoint, boolean roundTrip) {
		int totalPrice = 0;
		Location arrivalLocation = arrivalPoint.getLocation();
		Location departureLocation = departurePoint.getLocation();
		Terrain arrivalPlanet = arrivalLocation.getTerrain();
		Terrain departurePlanet = departureLocation.getTerrain();
		
		totalPrice += getTicketBasePrice(departurePlanet, arrivalPlanet);	// The base price
		totalPrice += Math.max(getAdditionalCost(departureLocation, arrivalLocation), 0);	// The extra amount to pay.
		
		if(roundTrip)
			totalPrice *= 2;
		
		// A factor smaller or equal to 0 makes the ticket free.
		if(ticketPriceFactor <= 0)
			totalPrice = 0;
		
		return totalPrice;
	}
	
	private void handleTicketPurchase(TicketPurchaseIntent i) {
		CreatureObject purchaser = i.getPurchaser();
		Location purchaserWorldLocation = purchaser.getWorldLocation();
		TravelPoint nearestPoint = getNearestTravelPoint(purchaserWorldLocation);
		TravelPoint destinationPoint = getDestinationPoint(Terrain.getTerrainFromName(i.getDestinationPlanet()), i.getDestinationName());
		Player purchaserOwner = purchaser.getOwner();
		boolean roundTrip = i.isRoundTrip();
		
		if (nearestPoint == null || destinationPoint == null) {
			Log.w(this, "Unable to purchase ticket! Nearest Point: %s  Destination Point: %s", nearestPoint, destinationPoint);
			return;
		}
		
		int ticketPrice = getTotalTicketPrice(nearestPoint, destinationPoint, roundTrip);
		int newBankBalance = purchaser.getBankBalance();
		int newCashBalance = purchaser.getCashBalance();
		int difference = newBankBalance - ticketPrice;
		
		if(difference < 0) {	// If they don't have enough credits in their bank
			newBankBalance = 0;	// They now have no more credits in their bank
			newCashBalance += difference;	// We subtract the remaining credits from their cash balance.
			
			if(newCashBalance < 0) {	// If they don't have enough bank and cash funds combined, they receive no ticket!
				// Make the message in the SUI window reflect the fail
				showMessageBox(purchaserOwner, "short_funds");
				return;
			}
		} else {
			newBankBalance = difference;
		}
		
		grantTickets(purchaser, nearestPoint, destinationPoint, roundTrip);
		handlePurchaseFinish(purchaserOwner, purchaser, ticketPrice, newCashBalance, newBankBalance);

	}
	
	private void handlePurchaseFinish(Player purchaserOwner, CreatureObject purchaser, int ticketPrice, long newCashBalance, long newBankBalance) {
		// Make the message in the SUI window reflect the success
		showMessageBox(purchaserOwner, "ticket_purchase_complete");
		
		// Also send the purchaser a system message
		// There's no StringId for this message.
		new ChatBroadcastIntent(purchaserOwner, String.format("You succesfully make a payment of %d credits to the Galactic Travel Commission.", ticketPrice)).broadcast();
		
		purchaser.setCashBalance(newCashBalance);
		purchaser.setBankBalance(newBankBalance);
	}
	
	private void grantTickets(CreatureObject purchaser, TravelPoint departurePoint, TravelPoint arrivalPoint, boolean roundTrip) {
		// Put the ticket in their inventory
		grantTicket(departurePoint, arrivalPoint, purchaser);
		
		if(roundTrip) {
			// Put the ticket in their inventory
			grantTicket(arrivalPoint, departurePoint, purchaser);
		}
	}
	
	private void showMessageBox(Player receiver, String message) {
		// Create the SUI window
		SuiMessageBox messageBox = new SuiMessageBox(SuiButtons.OK, "STAR WARS GALAXIES", "@travel:" + message);
		
		// Display the window to the purchaser
		messageBox.display(receiver);
	}
	
	private void grantTicket(TravelPoint departure, TravelPoint destination, SWGObject receiver) {
		// Create the ticket object
		SWGObject ticket = objectManager.createObject("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff");
		
		// Departure attributes
		ticket.addAttribute("@obj_attr_n:travel_departure_planet", "@planet_n:" + departure.getLocation().getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_departure_point", departure.getName());
		
		// Arrival attributes
		ticket.addAttribute("@obj_attr_n:travel_arrival_planet", "@planet_n:" + destination.getLocation().getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_arrival_point", destination.getName());
		
		ticket.moveToContainer(receiver.getSlottedObject("inventory"));
	}
	
	private void handleTicketUse(TicketUseIntent i) {
		TravelPoint point = getNearestTravelPoint(i.getPlayer().getCreatureObject().getWorldLocation());
		TravelGroup travel = null;
		if (point.getShuttle() == null) {
			Log.w(this, "No travel point shuttle near player: %s", i.getPlayer().getCreatureObject().getWorldLocation());
			return;
		}
		travel = this.travel.get(point.getShuttle().getTemplate());
		if (travel == null) {
			Log.e(this, "Travel point is null for shuttle: " + point.getShuttle());
			return;
		}
		switch (travel.getStatus()) {
			case GROUNDED:
				if (i.getTicket() == null)
					handleTicketUseSui(i);
				else
					handleTicketUseClick(i);
				break;
			case LANDING:
				new ChatBroadcastIntent(i.getPlayer(), "@travel/travel:shuttle_begin_boarding").broadcast();
				break;
			case LEAVING:
				new ChatBroadcastIntent(i.getPlayer(), "@travel:shuttle_not_available").broadcast();
				break;
			case AWAY:
				new ChatBroadcastIntent(i.getPlayer(), new ProsePackage(new StringId("travel/travel", "shuttle_board_delay"), "DI", travel.getTimeRemaining())).broadcast();
				break;
		}
	}
	
	private void handleTicketUseSui(TicketUseIntent i) {
		Player player = i.getPlayer();
		CreatureObject creature = player.getCreatureObject();
		Collection<SWGObject> tickets = creature.getItemsByTemplate("inventory", "object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff");
		List<SWGObject> usableTickets = new ArrayList<>();
		
		for (SWGObject ticket : tickets) {
			if (isTicket(ticket) && isTicketUsable(ticket))
				usableTickets.add(ticket);
		}
		
		if (usableTickets.isEmpty())	// They don't have a valid ticket.
			new ChatBroadcastIntent(player, "@travel:no_ticket_for_shuttle").broadcast();
		else {
			SuiListBox ticketBox = new SuiListBox(SuiButtons.OK_CANCEL, "@travel:select_destination", "@travel:select_destination");
			
			for(SWGObject usableTicket : usableTickets) {
				TravelPoint destinationPoint = getDestinationPoint(usableTicket);
				
				ticketBox.addListItem(destinationPoint.getSuiFormat(), destinationPoint);
			}
			
			ticketBox.addOkButtonCallback("handleSelectedItem", (callbackPlayer, actor, event, parameters) -> {
				handleTicketUse(callbackPlayer, usableTickets.get(SuiListBox.getSelectedRow(parameters)));
			});
			ticketBox.display(player);
		}
	}
	
	private void handleTicketUseClick(TicketUseIntent i) {
		handleTicketUse(i.getPlayer(), i.getTicket());
	}
	
	private void handleTicketUse(Player player, SWGObject ticket) {
		CreatureObject traveler = player.getCreatureObject();
		Location worldLoc = traveler.getWorldLocation();
		TravelPoint nearestPoint = getNearestTravelPoint(worldLoc);
		double distanceToNearestPoint = worldLoc.distanceTo(nearestPoint.getCollector().getWorldLocation());
		if (!isTicket(ticket)) {
			Log.e(this, "%s attempted to use an object that isn't a ticket!", player);
		} else if (nearestPoint.getGroup().getStatus() != ShuttleStatus.GROUNDED) {
			int time = nearestPoint.getGroup().getTimeRemaining();
			new ChatBroadcastIntent(player, new ProsePackage(new StringId("travel/travel", "shuttle_board_delay"), "DI", time)).broadcast();
		} else if (!isTicketUsable(ticket)) {
			// This ticket isn't valid for this point
			new ChatBroadcastIntent(player, "@travel:wrong_shuttle").broadcast();
		} else if (distanceToNearestPoint <= TICKET_USE_RADIUS) {
			// They can use their ticket if they're within range.
			Log.i(this, "%s/%s is traveling from %s to %s", player.getUsername(), traveler.getName(), nearestPoint.getName(), getDestinationPoint(ticket).getName());
			teleportAndDestroyTicket(getDestinationPoint(ticket), ticket, traveler);
		} else {
			new ChatBroadcastIntent(player, "@travel:boarding_too_far").broadcast();
		}
	}
	
	private void handleObjectCreation(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		// There are non-functional shuttles, which are StaticObject. We run an instanceof check to make sure that we ignore those.
		if (travel.containsKey(object.getTemplate()) && !(object instanceof StaticObject)) {
			Location shuttleLocation = object.getWorldLocation();
			TravelPoint pointForShuttle = getNearestTravelPoint(shuttleLocation);
			CreatureObject shuttle = (CreatureObject) object;
			
			if (pointForShuttle == null) {
				Log.w(this, "No point for shuttle at location: " + object.getWorldLocation());
				return;
			}
			// Assign the shuttle to the nearest travel point
			pointForShuttle.setShuttle(shuttle);
			
			shuttle.setOptionFlags(OptionFlag.INVULNERABLE);
			shuttle.setPosture(Posture.UPRIGHT);
			shuttle.setShownOnRadar(false);
		} else if (object.getTemplate().equals("object/tangible/travel/ticket_collector/shared_ticket_collector.iff")) {
			TravelPoint pointForCollector = getNearestTravelPoint(object.getWorldLocation());
			
			if (pointForCollector == null) {
				Log.w(this, "No point for collector at location: " + object.getWorldLocation());
				return;
			}
			
			pointForCollector.setCollector(object);
		}
	}
	
	private void teleportAndDestroyTicket(TravelPoint destination, SWGObject ticket, CreatureObject traveler) {
		objectManager.destroyObject(ticket);
		
		new ObjectTeleportIntent(traveler, destination.getLocation()).broadcast();
	}
	
	private boolean isTicket(SWGObject object) {
		String departurePlanet = object.getAttribute("@obj_attr_n:travel_departure_planet");
		String departureDestination = object.getAttribute("@obj_attr_n:travel_departure_point");
		String arrivalPlanet = object.getAttribute("@obj_attr_n:travel_arrival_planet");
		String arrivalPoint = object.getAttribute("@obj_attr_n:travel_arrival_point");
		
		return departurePlanet != null && departureDestination != null && arrivalPlanet != null && arrivalPoint != null;
	}
	
	private boolean isTicketUsable(SWGObject ticket) {
		Location worldLoc = ticket.getOwner().getCreatureObject().getWorldLocation();
		TravelPoint nearest = getNearestTravelPoint(worldLoc);
		String departurePoint = ticket.getAttribute("@obj_attr_n:travel_departure_point");
		String departurePlanet = ticket.getAttribute("@obj_attr_n:travel_departure_planet");
		Terrain departureTerrain = Terrain.getTerrainFromName(departurePlanet.split(":")[1]);
		Terrain currentTerrain = worldLoc.getTerrain();
		
		return departureTerrain == currentTerrain && departurePoint.equals(nearest.getName());
	}
	
	private TravelPoint getDestinationPoint(SWGObject ticket) {
		return getDestinationPoint(Terrain.getTerrainFromName(ticket.getAttribute("@obj_attr_n:travel_arrival_planet").split(":")[1]), ticket.getAttribute("@obj_attr_n:travel_arrival_point"));
	}
	
	private TravelPoint getDestinationPoint(Terrain terrain, String pointName) {
		for (TravelGroup gt : travel.values()) {
			TravelPoint point = gt.getDestination(terrain, pointName);
			if (point != null)
				return point;
		}
		return null;
	}
	
	private TravelPoint getNearestTravelPoint(Location objectLocation) {
		TravelPoint nearest = null;
		double dist = Double.MAX_VALUE;
		for (TravelGroup gt : travel.values()) {
			TravelPoint point = gt.getNearestPoint(objectLocation);
			if (point == null)
				continue;
			double d = point.getLocation().distanceTo(objectLocation);
			if (d < dist) {
				nearest = point;
				dist = d;
			}
		}
		return nearest;
	}
	
}
