/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.gameplay.world.travel

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.EnterTicketPurchaseModeMessage
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListRequest
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListResponse
import com.projectswg.common.network.packets.swg.zone.PlanetTravelPointListResponse.PlanetTravelPoint
import com.projectswg.holocore.intents.gameplay.world.TicketPurchaseIntent
import com.projectswg.holocore.intents.gameplay.world.TicketUseIntent
import com.projectswg.holocore.intents.gameplay.world.TravelPointSelectionIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.world.travel.TravelGroup.ShuttleStatus
import com.projectswg.holocore.resources.gameplay.world.travel.TravelHelper
import com.projectswg.holocore.resources.gameplay.world.travel.TravelPoint
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.SpecificObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.staticobject.StaticObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log

class TravelService : Service() {
	private val travel = TravelHelper()

	override fun start(): Boolean {
		travel.start()
		return super.start()
	}

	override fun stop(): Boolean {
		travel.stop()
		return super.stop()
	}

	@IntentHandler
	private fun handleTravelPointSelectionIntent(tpsi: TravelPointSelectionIntent) {
		val traveler = tpsi.creature

		traveler.sendSelf(EnterTicketPurchaseModeMessage(traveler.terrain.getName(), travel.getNearestTravelPoint(traveler)!!.name))
	}

	@IntentHandler
	private fun handleInboundPacketIntent(ipi: InboundPacketIntent) {
		val p = ipi.packet

		if (p is PlanetTravelPointListRequest) {
			val planetName = p.planetName
			val player = ipi.player
			val to = Terrain.getTerrainFromName(planetName)
			if (to == null) {
				Log.e("Unknown terrain in PlanetTravelPointListRequest: %s", planetName)
				return
			}
			val pointsForPlanet = travel.getAvailableTravelPoints(player.creatureObject, to)
			pointsForPlanet.sort()

			val nearest = travel.getNearestTravelPoint(player.creatureObject)
			if (nearest != null && pointsForPlanet.remove(nearest))
				pointsForPlanet.add(0, nearest) // Yes ... adding it to the beginning of the list because I hate the client

			val additionalCosts = getAdditionalCosts(nearest, pointsForPlanet)
			val pointList: MutableList<PlanetTravelPoint> = ArrayList()
			for (i in pointsForPlanet.indices) {
				val tp = pointsForPlanet[i]
				val cost = additionalCosts[i]
				pointList.add(PlanetTravelPoint(tp.name, tp.location.position, cost, tp.isReachable))
			}
			player.sendPacket(PlanetTravelPointListResponse(planetName, pointList))
		}
	}

	@IntentHandler
	private fun handleTicketPurchaseIntent(tpi: TicketPurchaseIntent) {
		val purchaser = tpi.purchaser
		val nearestPoint = travel.getNearestTravelPoint(purchaser)
		val destinationPoint = travel.getDestinationPoint(Terrain.getTerrainFromName(tpi.destinationPlanet), tpi.destinationName)
		val roundTrip = tpi.isRoundTrip

		if (nearestPoint == null || destinationPoint == null || !travel.isValidRoute(nearestPoint.terrain, destinationPoint.terrain)) {
			Log.w("Unable to purchase ticket! Nearest Point: %s  Destination Point: %s", nearestPoint, destinationPoint)
			return
		}

		val ticketPrice = getTotalTicketPrice(nearestPoint.terrain, destinationPoint.terrain, roundTrip)
		if (purchaser.removeFromBankAndCash(ticketPrice.toLong())) {
			sendTravelMessage(purchaser, String.format("You succesfully make a payment of %d credits to the Galactic Travel Commission.", ticketPrice))
		} else {
			showMessageBox(purchaser, "short_funds")
			return
		}

		travel.grantTicket(nearestPoint, destinationPoint, purchaser)
		if (roundTrip) travel.grantTicket(destinationPoint, nearestPoint, purchaser)
		showMessageBox(purchaser, "ticket_purchase_complete")
	}

	@IntentHandler
	private fun handleTicketUseIntent(tui: TicketUseIntent) {
		val creature = tui.player.creatureObject

		val nearestPoint = travel.getNearestTravelPoint(creature)
		if (nearestPoint?.shuttle == null || !nearestPoint.isWithinRange(creature)) {
			sendTravelMessage(creature, "@travel:boarding_too_far")
			return
		}

		when (nearestPoint.group!!.status) {
			ShuttleStatus.GROUNDED -> if (tui.ticket == null) handleTicketUseSui(tui.player)
			else travel.handleTicketUse(tui.player, tui.ticket, travel.getNearestTravelPoint(tui.ticket)!!, travel.getDestinationPoint(tui.ticket)!!)

			ShuttleStatus.LANDING  -> sendTravelMessage(creature, "@travel/travel:shuttle_begin_boarding")
			ShuttleStatus.LEAVING  -> sendTravelMessage(creature, "@travel:shuttle_not_available")
			ShuttleStatus.AWAY     -> sendTravelMessage(creature, "@travel/travel:shuttle_board_delay", "DI", nearestPoint.group.getTimeRemaining())
		}
	}

	@IntentHandler
	private fun handleObjectCreatedIntent(oci: ObjectCreatedIntent) {
		val obj = oci.obj


		// There are non-functional shuttles, which are StaticObject. We run an instanceof check to make sure that we ignore those.
		if (travel.getTravelGroup(obj.template) != null && obj !is StaticObject) {
			val pointForShuttle = travel.getNearestTravelPoint(obj)
			val shuttle = obj as CreatureObject

			if (pointForShuttle == null) {
				Log.w("No point for shuttle at location: " + obj.getWorldLocation())
				return
			}
			// Assign the shuttle to the nearest travel point
			pointForShuttle.shuttle = shuttle

			shuttle.setOptionFlags(OptionFlag.INVULNERABLE)
			shuttle.posture = Posture.UPRIGHT
		} else if (obj.template == SpecificObject.SO_TICKET_COLLETOR.template) {
			val pointForCollector = travel.getNearestTravelPoint(obj)

			if (pointForCollector == null) {
				Log.w("No point for collector at location: " + obj.worldLocation)
				return
			}

			pointForCollector.collector = obj
		} else if (obj is BuildingObject && (obj.getTemplate().contains("starport") || obj.getTemplate().contains("hangar"))) {
			val pointForCollector = travel.getNearestTravelPoint(obj)

			if (pointForCollector == null || pointForCollector.location.flatDistanceTo(obj.getLocation()) >= 50) return

			pointForCollector.starport = obj
		}
	}

	private fun getAdditionalCosts(departure: TravelPoint?, points: Collection<TravelPoint?>): List<Int> {
		val additionalCosts: MutableList<Int> = ArrayList()

		for (point in points) {
			if (point === departure) additionalCosts.add(-getClientCost(departure!!.terrain, point!!.terrain))
			else additionalCosts.add(-getClientCost(departure!!.terrain, point!!.terrain) + getTravelCost(departure.terrain, point.terrain))
		}

		return additionalCosts
	}

	private fun getClientCost(departure: Terrain, destination: Terrain): Int {
		return travel.getTravelFee(departure, destination) - 50
	}

	private fun getTravelCost(departure: Terrain, destination: Terrain): Int {
		return (travel.getTravelFee(departure, destination) * ticketPriceFactor).toInt()
	}

	private fun handleTicketUseSui(player: Player) {
		val usableTickets = travel.getTickets(player.creatureObject)

		if (usableTickets.isEmpty()) {    // They don't have a valid ticket.
			SystemMessageIntent(player, "@travel:no_ticket_for_shuttle").broadcast()
		} else if (usableTickets.size == 1) {
			val ticket = usableTickets[0]
			val nearestPoint = travel.getNearestTravelPoint(ticket)
			val destinationPoint = travel.getDestinationPoint(ticket)
			travel.handleTicketUse(player, ticket, nearestPoint!!, destinationPoint!!)
		} else {
			SuiListBox().run {
				title = "@travel:select_destination"
				prompt = "@travel:select_destination"

				for (usableTicket in usableTickets) {
					val destinationPoint = travel.getDestinationPoint(usableTicket)

					addListItem(destinationPoint!!.suiFormat, destinationPoint)
				}

				addOkButtonCallback("handleSelectedItem") { _: SuiEvent, parameters: Map<String, String> ->
					var row = SuiListBox.getSelectedRow(parameters)
					if (row < 0) {
						if (usableTickets.size == 1) row = 0
						else return@addOkButtonCallback
					}
					val ticket = usableTickets[row]
					val nearestPoint = travel.getNearestTravelPoint(ticket)
					val destinationPoint = getListItem(row).obj as TravelPoint?
					travel.handleTicketUse(player, ticket, nearestPoint!!, destinationPoint!!)
				}

				display(player)
			}
		}
	}

	private fun getTotalTicketPrice(departurePlanet: Terrain, arrivalPlanet: Terrain, roundTrip: Boolean): Int {
		var totalPrice = getTravelCost(departurePlanet, arrivalPlanet)

		if (roundTrip) totalPrice += getTravelCost(arrivalPlanet, departurePlanet)

		return totalPrice
	}

	private val ticketPriceFactor: Double
		get() = config.getDouble(this, "ticketPriceFactor", 1.0)

	private fun sendTravelMessage(creature: CreatureObject, message: String) {
		SystemMessageIntent(creature.owner!!, message).broadcast()
	}

	private fun sendTravelMessage(creature: CreatureObject, str: String, key: String, obj: Any) {
		SystemMessageIntent(creature.owner!!, ProsePackage(StringId(str), key, obj)).broadcast()
	}

	private fun showMessageBox(creature: CreatureObject, message: String) {
		SuiMessageBox().run {
			title = "STAR WARS GALAXIES"
			prompt = "@travel:$message"
			buttons = SuiButtons.OK
			display(creature.owner ?: return)
		}
	}
}
