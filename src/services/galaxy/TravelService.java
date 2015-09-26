package services.galaxy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import network.packets.Packet;
import network.packets.swg.zone.EnterTicketPurchaseModeMessage;
import network.packets.swg.zone.PlanetTravelPointListRequest;
import network.packets.swg.zone.PlanetTravelPointListResponse;
import intents.chat.ChatBroadcastIntent;
import intents.network.GalacticPacketIntent;
import intents.travel.TicketPurchaseIntent;
import intents.travel.TravelPointSelectionIntent;
import resources.Location;
import resources.Terrain;
import resources.TravelPoint;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.RelationalServerData;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;

public final class TravelService extends Service {
	
	private static final String DBTABLENAME = "travel";
	private static final String TRAVELPOINTSFORPLANET = "SELECT name, x, y, z FROM " + DBTABLENAME + " WHERE planet=";
	private static final byte PLANETNAMESCOLUMNINDEX = 0;
	
	private final ObjectManager objectManager;
	private final RelationalServerData travelPointDatabase;
	private final Map<Terrain, Collection<TravelPoint>> travelPoints;
	
	public TravelService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		
		travelPointDatabase = new RelationalServerData("serverdata/static/travel.db");
		travelPoints = new HashMap<>();
		
		if(!travelPointDatabase.linkTableWithSdb(DBTABLENAME, "serverdata/static/travel.sdb")) {
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for TravelService");
		}
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(TravelPointSelectionIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(TicketPurchaseIntent.TYPE);
		
		return super.initialize() && loadTravelPoints();
	}
	
	@Override
	public boolean terminate() {
		return super.terminate() && travelPointDatabase.close();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case TravelPointSelectionIntent.TYPE:	handlePointSelection((TravelPointSelectionIntent) i); break;
			case GalacticPacketIntent.TYPE:			handleTravelPointRequest((GalacticPacketIntent) i); break;
			case TicketPurchaseIntent.TYPE:			handleTicketPurchase((TicketPurchaseIntent) i); break;
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
		
		DatatableData travelFeeTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/travel/travel.iff");
		Map<String, Integer> planetFees;
		
		// Load the planet names
		String[] planetNames = new String[travelFeeTable.getRowCount()];
		
		travelFeeTable.handleRows(currentRow -> planetNames[currentRow] = (String) travelFeeTable.getCell(currentRow, PLANETNAMESCOLUMNINDEX));
		
		for(int i = 0; i < travelFeeTable.getRowCount(); i++) {
			String planetName = planetNames[i];
			
			try(ResultSet travelPointTable = travelPointDatabase.prepareStatement("'" + planetName + "'").executeQuery()) {
				planetFees = new HashMap<>();
				
				for(int j = 0; j < travelFeeTable.getRowCount(); j++) {
					planetFees.put((String) planetNames[j], (int) travelFeeTable.getCell(j, travelFeeTable.getColumnFromName(planetName)));
					System.out.println("Going from " + planetName + " to " + planetNames[j] + " costs " + travelFeeTable.getCell(j, travelFeeTable.getColumnFromName(planetName)));
				}
				Terrain terrain = Terrain.valueOf(planetName.toUpperCase(Locale.ENGLISH));
				
				while(travelPointTable.next()) {
					Location loc = new Location(travelPointTable.getDouble("x"), travelPointTable.getDouble("y"), travelPointTable.getDouble("z"), terrain);
					TravelPoint tp = new TravelPoint(travelPointTable.getString("name"), loc, planetFees, 0);
					Collection<TravelPoint> travelPointsForPlanet = travelPoints.get(terrain);
					
					if(travelPointsForPlanet == null) {
						travelPointsForPlanet = new ArrayList<>();
						
						travelPoints.put(terrain, travelPointsForPlanet);
					}
					
					travelPointsForPlanet.add(tp);
				}
			} catch(SQLException e) {
				e.printStackTrace();
				success = false;
			}
		}
		
		return success;
	}
	
	private void handlePointSelection(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), nearestTravelPoint(traveler.getWorldLocation()).getName(), tpsi.isInstant()));
	}
	
	private void handleTravelPointRequest(GalacticPacketIntent i) {
		Packet p = i.getPacket();
		
		if(p instanceof PlanetTravelPointListRequest ) {
			PlanetTravelPointListRequest req = (PlanetTravelPointListRequest) p;
			
			i.getPlayerManager().getPlayerFromNetworkId(i.getNetworkId()).sendPacket(new PlanetTravelPointListResponse(req.getPlanetName(), travelPoints.get(Terrain.valueOf(req.getPlanetName().toUpperCase(Locale.ENGLISH)))));
		}
	}
	
	private void handleTicketPurchase(TicketPurchaseIntent i) {
		CreatureObject purchaser = i.getPurchaser();
		Location purchaserWorldLocation = purchaser.getWorldLocation();
		TravelPoint nearestPoint = nearestTravelPoint(purchaserWorldLocation);
		TravelPoint destinationPoint = destinationPoint(purchaserWorldLocation.getTerrain(), i.getDestinationName());
		SWGObject ticket;
		int ticketPrice;
		SuiMessageBox messageBox;
		String suiMessage = "@travel:";
		Player purchaserOwner = purchaser.getOwner();
		int purchaserBankBalance;
		
		if(nearestPoint == null || destinationPoint == null)
			return;
		
		purchaserBankBalance = purchaser.getBankBalance();
		ticketPrice = nearestPoint.totalTicketPrice(i.getDestinationPlanet());
		
		if(ticketPrice > purchaserBankBalance) {
			// Make the message in the SUI window reflect the fail
			suiMessage += "short_funds";
		} else {
			// Make the message in the SUI window reflect the success
			suiMessage += "ticket_purchase_complete";
			
			// Also send the purchaser a system message
			// TODO is there a STF containing this?
			new ChatBroadcastIntent(purchaserOwner, String.format("You succesfully make a payment of %d credits to the Galactic Travel Commission.", ticketPrice)).broadcast();
			
			purchaser.setBankBalance(purchaserBankBalance - ticketPrice);
			
			// Create the ticket object
			ticket = objectManager.createObject("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff", false);
			
			// Departure attributes
			ticket.addAttribute("@obj_attr_n:travel_departure_planet", "@planet_n:" + purchaserWorldLocation.getTerrain().getName().toLowerCase(Locale.ENGLISH));
			ticket.addAttribute("@obj_attr_n:travel_departure_point", nearestPoint.getName());
			
			// Arrival attributes
			ticket.addAttribute("@obj_attr_n:travel_arrival_planet", "@planet_n:" + destinationPoint.getLocation().getTerrain().getName().toLowerCase(Locale.ENGLISH));
			ticket.addAttribute("@obj_attr_n:travel_arrival_point", destinationPoint.getName());
			
			// Put the ticket in their inventory
			purchaser.getSlottedObject("inventory").addObject(ticket);
		}
		
		// Create the SUI window
		messageBox = new SuiMessageBox(SuiButtons.OK, "STAR WARS GALAXIES", suiMessage);
		
		// Display the window to the purchaser
		messageBox.display(purchaserOwner);
	}
	
	private TravelPoint destinationPoint(Terrain terrain, String pointName) {
		TravelPoint currentResult = null;
		Iterator<TravelPoint> pointIterator = travelPoints.get(terrain).iterator();
		
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
		Collection<TravelPoint> pointsForPlanet = travelPoints.get(objectLocation.getTerrain());
		
		for(TravelPoint candidate : pointsForPlanet) {
			
			if(currentResult == null) { // Will occur upon the first iteration.
				currentResult = candidate; // The first candidate will always be the first possible result.
			} else {
				candidateDistance = candidate.getLocation().distanceTo(objectLocation);
				
				if(candidateDistance < currentResultDistance) {
					currentResult = candidate;
					currentResultDistance = candidateDistance;
				}
			}
		}
		return currentResult;
	}
	
}
