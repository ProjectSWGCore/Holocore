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
package services.galaxy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
import resources.objects.tangible.OptionFlag;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import resources.sui.ISuiCallback;
import resources.sui.SuiButtons;
import resources.sui.SuiEvent;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.SuiListBoxItem;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;
import utilities.ThreadUtilities;

public class TravelService extends Service {
	
	private static final String DB_TABLE_NAME = "travel";
	private static final byte PLANET_NAMES_COLUMN_INDEX = 0;
	private static final short TICKET_USE_RADIUS = 25;	// The distance a player needs to be within in order to use their ticket
	
	private final ObjectManager objectManager;
	private Terrain[] travelPlanets;
	private final Map<Terrain, Map<Terrain, Integer>> allowedRoutes; // Describes which planets are linked and base prices.
	private final DatatableData travelFeeTable;
	
	/**
	 * This variable stores all the loaded TravelPoints for a given planet.
	 * All TravelPoints on tatooine are mapped to {@code Terrain.TATOOINE}
	 */
	private final Map<Terrain, Collection<TravelPoint>> pointsOnPlanet;
	
	private final AtomicInteger groundTime;
	private final AtomicInteger airTime;
	private final double ticketPriceFactor;
	
	// Fields relating to shuttle take-off and landing
	private ExecutorService executor;
	private GalaxyTravel shuttleTravel;
	private GalaxyTravel transportTravel;
	
	public TravelService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		
		allowedRoutes = new HashMap<>();
		travelFeeTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/travel/travel.iff");
		pointsOnPlanet = new HashMap<>();
		
		ticketPriceFactor = getConfig(ConfigFile.FEATURES).getDouble("TICKET-PRICE-FACTOR", 1);
		groundTime = new AtomicInteger(getConfig(ConfigFile.FEATURES).getInt("SHUTTLE-GROUND-TIME", 120));
		airTime = new AtomicInteger(getConfig(ConfigFile.FEATURES).getInt("SHUTTLE-AIR-TIME", 60));
		
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
		executor = Executors.newFixedThreadPool(2, ThreadUtilities.newThreadFactory("travel-shuttles-%d"));
		shuttleTravel = new GalaxyTravel(true);
		transportTravel = new GalaxyTravel(false);
		executor.execute(shuttleTravel);
		executor.execute(transportTravel);
		
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
	 * @author Mads
	 * @return true if all points were loaded succesfully and false if not.
	 */
	private boolean loadTravelPoints() {
		boolean success = true;
		
		for(Terrain travelPlanet : allowedRoutes.keySet()) {
			String planetName = travelPlanet.getName();
			
			try(RelationalServerData data = RelationalServerFactory.getServerData("static/travel.db", DB_TABLE_NAME)) {
				try(ResultSet set = data.selectFromTable(DB_TABLE_NAME, null, "planet = ?", planetName)) {
					while(set.next()) {
						String pointName = set.getString("name");
						double x = set.getDouble("x");
						double y = set.getDouble("y");
						double z = set.getDouble("z");
						String type = set.getString("type");
						
						TravelPoint point = new TravelPoint(pointName, new Location(x, y, z, travelPlanet), type.equals("starport"), true);
						
						Collection<TravelPoint> pointsOnCurrentPlanet = pointsOnPlanet.get(travelPlanet);
						
						if(pointsOnCurrentPlanet == null) {
							pointsOnCurrentPlanet = new ArrayList<>();
							pointsOnPlanet.put(travelPlanet, pointsOnCurrentPlanet);
						}
						
						pointsOnCurrentPlanet.add(point);
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
	
	private Collection<TravelPoint> getPointsForPlanet(Location location, String planetName, boolean starport) {
		Collection<TravelPoint> points = new ArrayList<>();
		Terrain objectTerrain = location.getTerrain();
		Terrain destinationTerrain = Terrain.getTerrainFromName(planetName);
		
		Collection<TravelPoint> candidatePoints = pointsOnPlanet.get(destinationTerrain);
		
		for (TravelPoint candidatePoint : candidatePoints) {
			if (objectTerrain == destinationTerrain) {
				// If the destination planet is the same as our current
				points.addAll(candidatePoints);
				break;	// Then return all the points on this planet
			} else if (candidatePoint.isStarport() && starport) {	// If the terrains aren't the same, we only want to add the starports.
				points.add(candidatePoint);
			}
		}
		
		return points;
	}
	
	private void handlePointSelection(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), getNearestTravelPoint(traveler.getWorldLocation()).getName(), tpsi.isInstant()));
	}
	
	private void handleTravelPointRequest(GalacticPacketIntent i) {
		Packet p = i.getPacket();
		
		if (p instanceof PlanetTravelPointListRequest) {
			PlanetTravelPointListRequest req = (PlanetTravelPointListRequest) p;
			String planetName = req.getPlanetName();
			Player player = i.getPlayerManager().getPlayerFromNetworkId(i.getNetworkId());
			Location objectLocation = player.getCreatureObject().getWorldLocation();
			TravelPoint nearest = getNearestTravelPoint(objectLocation);
			Collection<TravelPoint> pointsForPlanet = getPointsForPlanet(objectLocation, planetName, nearest.isStarport());
			
			player.sendPacket(new PlanetTravelPointListResponse(planetName, pointsForPlanet, getAdditionalCosts(objectLocation, pointsForPlanet)));
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
		
		if (nearestPoint == null || destinationPoint == null)
			return;
		if (getDistanceFromPoint(nearestPoint, purchaserWorldLocation) > TICKET_USE_RADIUS) {
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
		boolean starport = point.isStarport();
		if (isShuttleAvailable(starport)) {
			// The shuttle is available at this time
			if(i.getTicket() == null)
				handleTicketUseSui(i);
			else
				handleTicketUseClick(i);
		} else { // The shuttle isn't available
			if (isShuttleBoarding(starport)) {	// Unavailable but about to board
				new ChatBroadcastIntent(i.getPlayer(), "@travel/travel:shuttle_begin_boarding").broadcast();
			} else {	// Unavailable but not about to board, because...
				if (isShuttleDeparting(starport))	// ... it's departing
					new ChatBroadcastIntent(i.getPlayer(), "@travel:shuttle_not_available").broadcast();
				else	// ... or it's done departing and is completely away
					new ChatBroadcastIntent(i.getPlayer(), new ProsePackage(new StringId("travel/travel", "shuttle_board_delay"), "DI", getTimeRemaining(starport))).broadcast();
			}

		}
	}
	
	private void handleTicketUseSui(TicketUseIntent i) {
		Player player = i.getPlayer();
		CreatureObject creature = player.getCreatureObject();
		Collection<SWGObject> tickets = creature.getItemsByTemplate("inventory", "object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff");
		SuiListBox destinationSelection;
		List<SWGObject> usableTickets = new ArrayList<>();
		
		for(SWGObject ticket : tickets)
			if(isTicket(ticket) && isTicketUsable(ticket))
				usableTickets.add(ticket);
		
		if(usableTickets.isEmpty())	// They don't have a valid ticket. 
			new ChatBroadcastIntent(player, "@travel:no_ticket_for_shuttle").broadcast();
		else {
			destinationSelection = new SuiListBox(SuiButtons.OK_CANCEL, "@travel:select_destination", "@travel:select_destination");
			
			for(SWGObject usableTicket : usableTickets) {
				TravelPoint destinationPoint = getDestinationPoint(usableTicket);
				
				destinationSelection.addListItem(destinationPoint.getSuiFormat(), destinationPoint);
			}
			
			destinationSelection.addOkButtonCallback("handleSelectedItem", new DestinationSelectionSuiCallback(destinationSelection, usableTickets));
			destinationSelection.display(player);
		}
	}
	
	private void handleTicketUseClick(TicketUseIntent i) {
		CreatureObject traveler = i.getPlayer().getCreatureObject();
		Location worldLoc = traveler.getWorldLocation();
		TravelPoint nearestPoint = getNearestTravelPoint(worldLoc);
		double distanceToNearestPoint = worldLoc.distanceTo(nearestPoint.getShuttle().getLocation());
		SWGObject ticket = i.getTicket();
		Player player = i.getPlayer();
		
		if (isTicket(ticket)) {
			if (isTicketUsable(ticket)) {
				if (distanceToNearestPoint <= TICKET_USE_RADIUS) {
					// They can use their ticket if they're within range.
					teleportAndDestroyTicket(getDestinationPoint(ticket), ticket, traveler);
				} else {
					// They're out of range - let them know.
					new ChatBroadcastIntent(player, "@travel:boarding_too_far").broadcast();
				}
			} else {
				// This ticket isn't valid for this point
				new ChatBroadcastIntent(player, "@travel:wrong_shuttle").broadcast();
			}
		}
	}
	
	private void handleObjectCreation(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		String template = object.getTemplate();
		
		// There are non-functional shuttles, which are StaticObject. We run an instanceof check to make sure that we ignore those.
		if((template.contains("shared_player_shuttle") || template.contains("shared_player_transport")) && object instanceof CreatureObject) {
			Location shuttleLocation = object.getLocation();
			TravelPoint pointForShuttle = getNearestTravelPoint(shuttleLocation);
			CreatureObject shuttle = (CreatureObject) object;
			
			// Assign the shuttle to the nearest travel point
			pointForShuttle.setShuttle(shuttle);
			
			shuttle.setOptionFlags(OptionFlag.INVULNERABLE);
			shuttle.setPosture(Posture.UPRIGHT);
			shuttle.setShownOnRadar(false);
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
		CreatureObject ticketOwner = ticket.getOwner().getCreatureObject();
		Location worldLoc = ticketOwner.getWorldLocation();
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
		TravelPoint currentResult = null;
		
		for(TravelPoint candidate : pointsOnPlanet.get(terrain)) {
			if(candidate.getName().equals(pointName)) {
				currentResult = candidate;
				break;
			}
		}
		
		return currentResult;
	}
	
	private TravelPoint getNearestTravelPoint(Location objectLocation) {
		TravelPoint currentResult = null;
		double currentResultDistance = Double.MAX_VALUE;
		double candidateDistance;
		Collection<TravelPoint> pointsForPlanet = pointsOnPlanet.get(objectLocation.getTerrain());
		
		if (pointsForPlanet == null)
			return null;
		
		for (TravelPoint candidate : pointsForPlanet) {
			if (currentResult == null) { // Will occur upon the first iteration.
				currentResult = candidate; // The first candidate will always be the first possible result.
				currentResultDistance = getDistanceFromPoint(currentResult, objectLocation);
			} else {
				candidateDistance = getDistanceFromPoint(candidate, objectLocation);
				
				if(candidateDistance < currentResultDistance) {
					currentResult = candidate;
					currentResultDistance = candidateDistance;
				}
			}
		}
		
		return currentResult;
	}

	private double getDistanceFromPoint(TravelPoint point, Location objectLocation) {
		return point.getLocation().distanceTo(objectLocation);
	}
	
	private void updateShuttlePostures(boolean starport, boolean landed) {
		for (Collection<TravelPoint> travelPoints : pointsOnPlanet.values()) {
			for (TravelPoint tp : travelPoints) {
				CreatureObject shuttle = tp.getShuttle();
				
				if(shuttle == null)	// This TravelPoint has no associated shuttle
					continue;	// Continue with the next TravelPoint
				
				if (shuttle.getTemplate().contains("shared_player_shuttle") && starport)
					continue;
				if (shuttle.getTemplate().contains("shared_player_transport") && !starport)
					continue;
				
				shuttle.setPosture(landed ? Posture.UPRIGHT : Posture.PRONE);
			}
		}
	}
	
	private void landShuttles(boolean starport) {
		updateShuttlePostures(starport, true);
	}
	
	private void launchShuttles(boolean starport) {
		updateShuttlePostures(starport, false);
	}
	
	private boolean isShuttleAvailable(boolean starport) {
		if (starport)
			return transportTravel.isShuttleAvailable();
		else
			return shuttleTravel.isShuttleAvailable();
	}
	
	private boolean isShuttleBoarding(boolean starport) {
		if (starport)
			return transportTravel.isShuttleBoarding();
		else
			return shuttleTravel.isShuttleBoarding();
	}
	
	private boolean isShuttleDeparting(boolean starport) {
		if (starport)
			return transportTravel.isShuttleDeparting();
		else
			return shuttleTravel.isShuttleDeparting();
	}
	
	private int getTimeRemaining(boolean starport) {
		if (starport)
			return transportTravel.getTimeRemaining();
		else
			return shuttleTravel.getTimeRemaining();
	}
	
	private class DestinationSelectionSuiCallback implements ISuiCallback {

		private final SuiListBox destinationSelection;
		private final List<SWGObject> usableTickets;
		
		private DestinationSelectionSuiCallback(SuiListBox destinationSelection, List<SWGObject> usableTickets) {
			this.destinationSelection = destinationSelection;
			this.usableTickets = usableTickets;
		}
		
		@Override
		public void handleEvent(Player player, SWGObject actor, SuiEvent event, Map<String, String> parameters) {
			int selection = SuiListBox.getSelectedRow(parameters);
			SuiListBoxItem selectedItem = destinationSelection.getListItem(selection);
			TravelPoint selectedDestination = (TravelPoint) selectedItem.getObject();
			
			teleportAndDestroyTicket(selectedDestination, usableTickets.get(selection), player.getCreatureObject());
		}
	}
	
	private class GalaxyTravel implements Runnable {
		
		private final AtomicInteger timeRemaining;
		private final AtomicBoolean shuttleLanded;
		private final AtomicBoolean postureLanded;
		private final boolean starport;
		private final long landTime;
		
		public GalaxyTravel(boolean starport) {
			this.starport = starport;
			landTime = (starport ? 21000 : 17000) + 10000; // Adds time for delta to take effect
			timeRemaining = new AtomicInteger(airTime.get());
			shuttleLanded = new AtomicBoolean(true);
			postureLanded = new AtomicBoolean(true);
		}
		
		public int getTimeRemaining() {
			return timeRemaining.get();
		}
		
		public boolean isShuttleAvailable() {
			return postureLanded.get() && shuttleLanded.get();
		}
		
		public boolean isShuttleBoarding() {
			return postureLanded.get() && !shuttleLanded.get();
		}
		
		public boolean isShuttleDeparting() {
			return !postureLanded.get() && !shuttleLanded.get();
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(50);
				while (true) {
					// LANDING
					if (!postureLanded.get()) {
						landShuttles(starport);
						postureLanded.set(true);
						Thread.sleep(landTime);
					}
					
					// GROUNDED
					shuttleLanded.set(true);
					Thread.sleep(groundTime.get() * 1000L);
					shuttleLanded.set(false);
					
					// LEAVE
					if (postureLanded.get()) {
						launchShuttles(starport);
						postureLanded.set(false);
						Thread.sleep(landTime);
					}
					
					// AWAY
					for (int timeElapsed = 0; timeElapsed < airTime.get(); timeElapsed++) {
						Thread.sleep(1000);	// Sleep for a second
						timeRemaining.decrementAndGet();
					}
					timeRemaining.set(airTime.get());	// Reset the timer
				}
			} catch (InterruptedException e) {
				
			}
		}
	}
}
