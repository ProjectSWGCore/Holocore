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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.sui.ISuiCallback;
import resources.sui.SuiButtons;
import resources.sui.SuiEvent;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.SuiListBoxItem;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;

public final class TravelService extends Service {
	
	private static final String DB_TABLE_NAME = "travel";
	private static final String TRAVEL_POINTS_FOR_PLANET = "SELECT * FROM " + DB_TABLE_NAME + " WHERE planet=";
	private static final byte PLANET_NAMES_COLUMN_INDEX = 0;
	private static final short TICKET_USE_RADIUS = 25;	// The distance a player needs to be within in order to use their ticket
	
	private final ObjectManager objectManager;
	private final RelationalServerData travelPointDatabase;
	private Terrain[] travelPlanets;
	private final Map<Terrain, Map<Terrain, Integer>> allowedRoutes; // Describes which planets are linked and base prices.
	private final DatatableData travelFeeTable;
	
	/**
	 * This variable stores all the loaded TravelPoints for a given planet.
	 * All TravelPoints on tatooine are mapped to {@code Terrain.TATOOINE}
	 */
	private final Map<Terrain, Collection<TravelPoint>> pointsOnPlanet;
	
	// Fields relating to shuttle take-off and landing
	private Posture currentShuttlePosture;
	private final ExecutorService executor;
	private final int timeUntilLand;
	private int timeRemaining;
	private final long landDelay;
	private boolean shuttleLanded;
	private final long timeGrounded;
	
	public TravelService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		
		travelPointDatabase = new RelationalServerData("serverdata/static/travel.db");
		
		if(!travelPointDatabase.linkTableWithSdb(DB_TABLE_NAME, "serverdata/static/travel.sdb")) {
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for TravelService");
		}
		
		allowedRoutes = new HashMap<>();
		travelFeeTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/travel/travel.iff");
		pointsOnPlanet = new HashMap<>();
		
		executor = Executors.newSingleThreadExecutor();
		timeUntilLand = getConfig(ConfigFile.FEATURES).getInt("SHUTTLE-AWAY-TIME", 60);
		timeRemaining = timeUntilLand;
		shuttleLanded = true;	// Shuttles start off being grounded
		currentShuttlePosture = Posture.UPRIGHT;
		landDelay = 10000;	// debugging
		timeGrounded = 120000;
		
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
		executor.execute(new ShuttleBehaviour());
		
		return super.start();
	}
	
	@Override
	public boolean stop() {
		executor.shutdown();
		
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		travelPointDatabase.close();
		
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case TravelPointSelectionIntent.TYPE:	handlePointSelection((TravelPointSelectionIntent) i); break;
			case GalacticPacketIntent.TYPE:			handleTravelPointRequest((GalacticPacketIntent) i); break;
			case TicketPurchaseIntent.TYPE:			handleTicketPurchase((TicketPurchaseIntent) i); break;
			case TicketUseIntent.TYPE:				handleTicketUse((TicketUseIntent) i); break;
			case ObjectCreatedIntent.TYPE:			handleObjectCreation((ObjectCreatedIntent) i); break;
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
				
				if(price > 0) {	// If price is above 0, the planets are linked
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
			
			try(ResultSet set = travelPointDatabase.prepareStatement(TRAVEL_POINTS_FOR_PLANET + "'" + planetName + "'").executeQuery()) {
				while(set.next()) {
					String pointName = set.getString("name");
					double x = set.getDouble("x");
					double y = set.getDouble("y");
					double z = set.getDouble("z");
					String type = set.getString("type");
					
					TravelPoint point = new TravelPoint(pointName, new Location(x, y, z, travelPlanet), 0, type.equals("starport"), true);
					
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
		
		return success;
	}
	
	private Collection<TravelPoint> pointsForPlanet(Location location, String planetName) {
		Collection<TravelPoint> points = new ArrayList<>();
		Terrain objectTerrain = location.getTerrain();
		Terrain destinationTerrain = Terrain.getTerrainFromName(planetName);
		
		Collection<TravelPoint> candidatePoints = pointsOnPlanet.get(destinationTerrain);
		
		for(TravelPoint candidatePoint : candidatePoints) {
			if(objectTerrain == destinationTerrain) {
				// If the destination planet is the same as our current
				points.addAll(candidatePoints);
				break;	// Then return all the points on this planet
			} else {
				 if(candidatePoint.isStarport()) {	// If the terrains aren't the same, we only want to add the starports.
					 points.add(candidatePoint);
				 }
			}
		}
		
		return points;
	}
	
	private void handlePointSelection(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), nearestTravelPoint(traveler.getWorldLocation()).getName(), tpsi.isInstant()));
	}
	
	private void handleTravelPointRequest(GalacticPacketIntent i) {
		Packet p = i.getPacket();
		
		if(p instanceof PlanetTravelPointListRequest ) {
			PlanetTravelPointListRequest req = (PlanetTravelPointListRequest) p;
			String planetName = req.getPlanetName();
			Player player = i.getPlayerManager().getPlayerFromNetworkId(i.getNetworkId());
			
			player.sendPacket(new PlanetTravelPointListResponse(planetName, pointsForPlanet(player.getCreatureObject().getWorldLocation(), planetName)));
		}
	}
	
	private int getTotalTicketPrice(TravelPoint departurePoint, Terrain arrivalPlanet) {
		int totalPrice = 0;
		Terrain departurePlanet = departurePoint.getLocation().getTerrain();
		
		totalPrice += allowedRoutes.get(departurePlanet).get(arrivalPlanet);
		totalPrice += departurePoint.getAdditionalCost();
		
		return totalPrice;
	}
	
	private void handleTicketPurchase(TicketPurchaseIntent i) {
		CreatureObject purchaser = i.getPurchaser();
		Location purchaserWorldLocation = purchaser.getWorldLocation();
		TravelPoint nearestPoint = nearestTravelPoint(purchaserWorldLocation);
		TravelPoint destinationPoint = getDestinationPoint(Terrain.getTerrainFromName(i.getDestinationPlanet()), i.getDestinationName());
		Player purchaserOwner = purchaser.getOwner();
		boolean roundTrip = i.isRoundTrip();
		
		if(nearestPoint == null || destinationPoint == null)
			return;
		
		int ticketPrice = getTotalTicketPrice(nearestPoint, destinationPoint.getLocation().getTerrain());
		int newBankBalance = purchaser.getBankBalance();
		int newCashBalance = purchaser.getCashBalance();
		
		if(roundTrip)
			ticketPrice *= 2;
		
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
		
		// Make the message in the SUI window reflect the success
		showMessageBox(purchaserOwner, "ticket_purchase_complete");
		
		// Also send the purchaser a system message
		// There's no StringId for this message.
		new ChatBroadcastIntent(purchaserOwner, String.format("You succesfully make a payment of %d credits to the Galactic Travel Commission.", ticketPrice)).broadcast();
		
		purchaser.setCashBalance(newCashBalance);
		purchaser.setBankBalance(newBankBalance);
		
		// Put the ticket in their inventory
		grantTicket(nearestPoint, destinationPoint, purchaser);
		
		if(roundTrip) {
			// Put the ticket in their inventory
			grantTicket(destinationPoint, nearestPoint, purchaser);
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
		SWGObject ticket = objectManager.createObject("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff", false);
		
		// Departure attributes
		ticket.addAttribute("@obj_attr_n:travel_departure_planet", "@planet_n:" + departure.getLocation().getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_departure_point", departure.getName());
		
		// Arrival attributes
		ticket.addAttribute("@obj_attr_n:travel_arrival_planet", "@planet_n:" + destination.getLocation().getTerrain().getName());
		ticket.addAttribute("@obj_attr_n:travel_arrival_point", destination.getName());
		
		receiver.getSlottedObject("inventory").addObject(ticket);
	}
	
	private void handleTicketUse(TicketUseIntent i) {
		if(isShuttleAvailable()) {
			// The shuttle is available at this time
			if(i.getTicket() == null)
				handleTicketUseSui(i);
			else
				handleTicketUseClick(i);
		} else { // The shuttle isn't available
			if(isShuttleBoarding()) {	// Unavailable but about to board
				new ChatBroadcastIntent(i.getPlayer(), "@travel/travel:shuttle_begin_boarding").broadcast();
			} else {	// Unavailable but not about to board, because...
				if(isShuttleDeparting())	// ... it's departing
					new ChatBroadcastIntent(i.getPlayer(), "@travel:shuttle_not_available").broadcast();
				else	// ... or it's done departing and is completely away
					new ChatBroadcastIntent(i.getPlayer(), new ProsePackage(new StringId("travel/travel", "shuttle_board_delay"), "DI", timeRemaining)).broadcast();
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
		TravelPoint nearestPoint = nearestTravelPoint(worldLoc);
		double distanceToNearestPoint = worldLoc.distanceTo(nearestPoint.getShuttle().getLocation());
		SWGObject ticket = i.getTicket();
		Player player = i.getPlayer();
		
		if(isTicket(ticket)) {
			if(isTicketUsable(ticket)) {
				if(distanceToNearestPoint <= TICKET_USE_RADIUS) {
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
		
		if(template.contains("shared_player_shuttle") || template.contains("shared_player_transport")) {
			CreatureObject shuttle = (CreatureObject) object;
			Location shuttleLocation = shuttle.getLocation();
			TravelPoint pointForShuttle = nearestTravelPoint(shuttleLocation);
			
			// Assign the shuttle to the nearest travel point
			pointForShuttle.setShuttle(shuttle);
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
		TravelPoint nearest = nearestTravelPoint(worldLoc);
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
		Iterator<TravelPoint> pointIterator = pointsOnPlanet.get(terrain).iterator();
		
		while(pointIterator.hasNext()) {
			TravelPoint candidate = pointIterator.next();
			
			if(candidate.getName().equals(pointName)) {
				currentResult = candidate;
				break;
			}
		}
		
		return currentResult;
	}
	
	private TravelPoint nearestTravelPoint(Location objectLocation) {
		TravelPoint currentResult = null;
		double currentResultDistance = Double.MAX_VALUE;
		double candidateDistance;
		Collection<TravelPoint> pointsForPlanet = pointsOnPlanet.get(objectLocation.getTerrain());
		
		for(TravelPoint candidate : pointsForPlanet) {
			if(currentResult == null) { // Will occur upon the first iteration.
				currentResult = candidate; // The first candidate will always be the first possible result.
				currentResultDistance = distanceFromPoint(currentResult, objectLocation);
			} else {
				candidateDistance = distanceFromPoint(candidate, objectLocation);
				
				if(candidateDistance < currentResultDistance) {
					currentResult = candidate;
					currentResultDistance = candidateDistance;
				}
			}
		}
		
		return currentResult;
	}

	private double distanceFromPoint(TravelPoint point, Location objectLocation) {
		return point.getLocation().distanceTo(objectLocation);
	}
	
	private void updateShuttlePostures(Posture posture) {
		for(Collection<TravelPoint> travelPoints : pointsOnPlanet.values()) {
			for(TravelPoint tp : travelPoints) {
				CreatureObject shuttle = tp.getShuttle();
				
				if(shuttle == null)	// This TravelPoint has no associated shuttle
					continue;	// Continue with the next TravelPoint
				
				shuttle.setPosture(posture);
			}
		}
		
		currentShuttlePosture = posture;
	}
	
	private boolean isShuttleAvailable() {
		return currentShuttlePosture == Posture.UPRIGHT && shuttleLanded;
	}
	
	private boolean isShuttleBoarding() {
		return currentShuttlePosture == Posture.UPRIGHT && !shuttleLanded;
	}
	
	private boolean isShuttleDeparting() {
		return currentShuttlePosture == Posture.PRONE && !shuttleLanded && timeRemaining == timeUntilLand;
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
	
	private class ShuttleBehaviour implements Runnable {
		@Override
		public void run() {
			while(true) {
				try {
					// GROUNDED
					Thread.sleep(timeGrounded);
					
					// LEAVE
					updateShuttlePostures(Posture.PRONE);
					shuttleLanded = false;
					Thread.sleep(landDelay);
					
					// Make the shuttle stay away for some time.
					for(long timeElapsed = 0; timeUntilLand > timeElapsed; timeRemaining--, timeElapsed++) {
						Thread.sleep(1000);	// Sleep for a second
					}
					
					timeRemaining = timeUntilLand;	// Reset the timer
					
					// LANDING
					updateShuttlePostures(Posture.UPRIGHT);
					Thread.sleep(landDelay);
					shuttleLanded = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
