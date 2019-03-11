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
package com.projectswg.holocore.services.gameplay.world.travel;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.EnterTicketPurchaseModeMessage;
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListRequest;
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListResponse;
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListResponse.PlanetTravelPoint;
import com.projectswg.holocore.intents.gameplay.world.travel.TicketPurchaseIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.TicketUseIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.TravelPointSelectionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.world.travel.TravelHelper;
import com.projectswg.holocore.resources.gameplay.world.travel.TravelPoint;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.SpecificObject;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.staticobject.StaticObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TravelService extends Service {
	
	private final TravelHelper travel;
	
	public TravelService() {
		this.travel = new TravelHelper();
	}
	
	@Override
	public boolean start() {
		travel.start();
		return super.start();
	}
	
	@Override
	public boolean stop() {
		travel.stop();
		return super.stop();
	}
	
	@IntentHandler
	private void handleTravelPointSelectionIntent(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), travel.getNearestTravelPoint(traveler).getName(), tpsi.isInstant()));
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		SWGPacket p = ipi.getPacket();
		
		if (p instanceof PlanetTravelPointListRequest) {
			String planetName = ((PlanetTravelPointListRequest) p).getPlanetName();
			Player player = ipi.getPlayer();
			Terrain to = Terrain.getTerrainFromName(planetName);
			if (to == null) {
				Log.e("Unknown terrain in PlanetTravelPointListRequest: %s", planetName);
				return;
			}
			List<TravelPoint> pointsForPlanet = travel.getAvailableTravelPoints(player.getCreatureObject(), to);
			Collections.sort(pointsForPlanet);
			
			TravelPoint nearest = travel.getNearestTravelPoint(player.getCreatureObject());
			if (pointsForPlanet.remove(nearest))
				pointsForPlanet.add(0, nearest); // Yes ... adding it to the beginning of the list because I hate the client
			
			List<Integer> additionalCosts = getAdditionalCosts(nearest, pointsForPlanet);
			List<PlanetTravelPoint> pointList = new ArrayList<>();
			for (int i = 0; i < pointsForPlanet.size(); i++) {
				TravelPoint tp = pointsForPlanet.get(i);
				int cost = additionalCosts.get(i);
				pointList.add(new PlanetTravelPoint(tp.getName(), tp.getLocation().getPosition(), cost, tp.isReachable()));
			}
			player.sendPacket(new PlanetTravelPointListResponse(planetName, pointList));
		}
	}
	
	@IntentHandler
	private void handleTicketPurchaseIntent(TicketPurchaseIntent tpi) {
		CreatureObject purchaser = tpi.getPurchaser();
		TravelPoint nearestPoint = travel.getNearestTravelPoint(purchaser);
		TravelPoint destinationPoint = travel.getDestinationPoint(Terrain.getTerrainFromName(tpi.getDestinationPlanet()), tpi.getDestinationName());
		boolean roundTrip = tpi.isRoundTrip();
		
		if (nearestPoint == null || destinationPoint == null || !travel.isValidRoute(nearestPoint.getTerrain(), destinationPoint.getTerrain())) {
			Log.w("Unable to purchase ticket! Nearest Point: %s  Destination Point: %s", nearestPoint, destinationPoint);
			return;
		}
		
		int ticketPrice = getTotalTicketPrice(nearestPoint.getTerrain(), destinationPoint.getTerrain(), roundTrip);
		if (purchaser.removeFromBankAndCash(ticketPrice)) {
			sendTravelMessage(purchaser, String.format("You succesfully make a payment of %d credits to the Galactic Travel Commission.", ticketPrice));
		} else {
			showMessageBox(purchaser, "short_funds");
			return;
		}
		
		travel.grantTicket(nearestPoint, destinationPoint, purchaser);
		if (roundTrip)
			travel.grantTicket(destinationPoint, nearestPoint, purchaser);
		showMessageBox(purchaser, "ticket_purchase_complete");
	}
	
	@IntentHandler
	private void handleTicketUseIntent(TicketUseIntent tui) {
		CreatureObject creature = tui.getPlayer().getCreatureObject();
		
		TravelPoint nearestPoint = travel.getNearestTravelPoint(creature);
		if (nearestPoint == null || nearestPoint.getShuttle() == null || !nearestPoint.isWithinRange(creature)) {
			sendTravelMessage(creature, "@travel:boarding_too_far");
			return;
		}
		
		switch (nearestPoint.getGroup().getStatus()) {
			case GROUNDED:
				if (tui.getTicket() == null)
					handleTicketUseSui(tui.getPlayer());
				else
					travel.handleTicketUse(tui.getPlayer(), tui.getTicket(), travel.getNearestTravelPoint(tui.getTicket()), travel.getDestinationPoint(tui.getTicket()));
				break;
			case LANDING:
				sendTravelMessage(creature, "@travel/travel:shuttle_begin_boarding");
				break;
			case LEAVING:
				sendTravelMessage(creature, "@travel:shuttle_not_available");
				break;
			case AWAY:
				sendTravelMessage(creature, "@travel/travel:shuttle_board_delay", "DI", nearestPoint.getGroup().getTimeRemaining());
				break;
		}
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		
		// There are non-functional shuttles, which are StaticObject. We run an instanceof check to make sure that we ignore those.
		if (travel.getTravelGroup(object.getTemplate()) != null && !(object instanceof StaticObject)) {
			TravelPoint pointForShuttle = travel.getNearestTravelPoint(object);
			CreatureObject shuttle = (CreatureObject) object;
			
			if (pointForShuttle == null) {
				Log.w("No point for shuttle at location: " + object.getWorldLocation());
				return;
			}
			// Assign the shuttle to the nearest travel point
			pointForShuttle.setShuttle(shuttle);
			
			shuttle.setOptionFlags(OptionFlag.INVULNERABLE);
			shuttle.setPosture(Posture.UPRIGHT);
			shuttle.setShownOnRadar(false);
		} else if (object.getTemplate().equals(SpecificObject.SO_TICKET_COLLETOR.getTemplate())) {
			TravelPoint pointForCollector = travel.getNearestTravelPoint(object);
			
			if (pointForCollector == null) {
				Log.w("No point for collector at location: " + object.getWorldLocation());
				return;
			}
			
			pointForCollector.setCollector(object);
		}
	}
	
	private List<Integer> getAdditionalCosts(TravelPoint departure, Collection<TravelPoint> points) {
		List<Integer> additionalCosts = new ArrayList<>();
		
		for (TravelPoint point : points) {
			if (point == departure)
				additionalCosts.add(-getClientCost(departure.getTerrain(), point.getTerrain()));
			else
				additionalCosts.add(-getClientCost(departure.getTerrain(), point.getTerrain()) + getTravelCost(departure.getTerrain(), point.getTerrain()));
		}
		
		return additionalCosts;
	}
	
	private int getClientCost(Terrain departure, Terrain destination) {
		return travel.getTravelFee(departure, destination) - 50;
	}
	
	private int getTravelCost(Terrain departure, Terrain destination) {
		return (int) (travel.getTravelFee(departure, destination) * getTicketPriceFactor());
	}
	
	private void handleTicketUseSui(Player player) {
		List<SWGObject> usableTickets = travel.getTickets(player.getCreatureObject());
		
		if (usableTickets.isEmpty()) {	// They don't have a valid ticket.
			new SystemMessageIntent(player, "@travel:no_ticket_for_shuttle").broadcast();
		} else {
			SuiListBox ticketBox = new SuiListBox(SuiButtons.OK_CANCEL, "@travel:select_destination", "@travel:select_destination");
			
			for (SWGObject usableTicket : usableTickets) {
				TravelPoint destinationPoint = travel.getDestinationPoint(usableTicket);
				
				ticketBox.addListItem(destinationPoint.getSuiFormat(), destinationPoint);
			}
			
			ticketBox.addOkButtonCallback("handleSelectedItem", (event, parameters) -> {
				int row = SuiListBox.getSelectedRow(parameters);
				SWGObject ticket = usableTickets.get(row);
				TravelPoint nearestPoint = travel.getNearestTravelPoint(ticket);
				TravelPoint destinationPoint = (TravelPoint) ticketBox.getListItem(row).getObject();
				travel.handleTicketUse(player, ticket, nearestPoint, destinationPoint);
			});
			ticketBox.display(player);
		}
	}
	
	private int getTotalTicketPrice(Terrain departurePlanet, Terrain arrivalPlanet, boolean roundTrip) {
		int totalPrice = getTravelCost(departurePlanet, arrivalPlanet);
		
		if (roundTrip)
			totalPrice += getTravelCost(arrivalPlanet, departurePlanet);
		
		return totalPrice;
	}
	
	private double getTicketPriceFactor() {
		return PswgDatabase.config().getDouble(this, "ticketPriceFactor", 1);
	}
	
	private void sendTravelMessage(CreatureObject creature, String message) {
		new SystemMessageIntent(creature.getOwner(), message).broadcast();
	}
	
	private void sendTravelMessage(CreatureObject creature, String str, String key, Object obj) {
		new SystemMessageIntent(creature.getOwner(), new ProsePackage(new StringId(str), key, obj)).broadcast();
	}
	
	private void showMessageBox(CreatureObject creature, String message) {
		// Create the SUI window
		SuiMessageBox messageBox = new SuiMessageBox(SuiButtons.OK, "STAR WARS GALAXIES", "@travel:" + message);
		
		// Display the window to the purchaser
		messageBox.display(creature.getOwner());
	}
	
}
