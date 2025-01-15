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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.world.travel.TravelGroup.ShuttleStatus
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.travelCosts
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.SpecificObject
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TicketInformation
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class TravelHelper {
	private val travel: MutableMap<String, TravelGroup> = ConcurrentHashMap()
	private val coroutineScope = HolocoreCoroutine.childScope()
	private val pointContainer = TravelPointContainer()

	init {
		createGalaxyTravels()
		loadTravelPoints()
	}

	fun start() {
		for (group in travel.values) group.launch(coroutineScope)
	}

	fun stop() {
		coroutineScope.cancelAndWait()
	}

	fun addTravelPoint(point: TravelPoint) {
		pointContainer.addTravelPoint(point)
	}

	fun isValidRoute(departure: Terrain, destination: Terrain): Boolean {
		return getTravelFee(departure, destination) != 0
	}

	fun getTravelFee(departure: Terrain, destination: Terrain): Int {
		return travelCosts().getCost(departure, destination)
	}

	fun getTravelGroup(template: String): TravelGroup? {
		return travel[template]
	}

	fun getAvailableTravelPoints(traveller: SWGObject, terrain: Terrain): MutableList<TravelPoint> {
		if (!isValidRoute(traveller.terrain, terrain)) return ArrayList()
		val nearest = getNearestTravelPoint(traveller)
		return pointContainer.getPointsForTerrain(nearest, terrain)
	}

	fun getDestinationPoint(ticket: TangibleObject): TravelPoint? {
		val ticketInformation = ticket.ticketInformation
		val planet = ticketInformation.arrivalPlanet
		val point = ticketInformation.arrivalPoint
		return getDestinationPoint(planet, point)
	}

	fun getDestinationPoint(terrain: Terrain, pointName: String): TravelPoint? {
		return pointContainer.getDestination(terrain, pointName)
	}

	fun getNearestTravelPoint(obj: SWGObject): TravelPoint? {
		return pointContainer.getNearestPoint(obj.worldLocation)
	}

	fun grantTicket(departure: TravelPoint, destination: TravelPoint, receiver: SWGObject) {
		// Create the ticket object
		val ticket = SpecificObject.SO_TRAVEL_TICKET.createType() as TangibleObject
		Log.t("Granting ticket for departure: %s/%s  and destination: %s/%s", departure.location, departure.name, destination.location, destination.name)

		val ticketInformation = TicketInformation()
		// Departure attributes
		ticketInformation.departurePlanet = departure.terrain
		ticketInformation.departurePoint = departure.name


		// Arrival attributes
		ticketInformation.arrivalPlanet = destination.terrain
		ticketInformation.arrivalPoint = destination.name

		ticket.ticketInformation = ticketInformation
		ticket.moveToContainer(receiver.getSlottedObject("inventory"))
		ObjectCreatedIntent(ticket).broadcast()
	}

	fun getTickets(creature: CreatureObject): List<TangibleObject> {
		val tickets = creature.getItemsByTemplate("inventory", SpecificObject.SO_TRAVEL_TICKET.template)
		val usableTickets: MutableList<TangibleObject> = ArrayList()

		for (ticket in tickets) {
			if (ticket is TangibleObject) {
				if (isTicket(ticket) && isTicketUsable(ticket, getNearestTravelPoint(ticket))) usableTickets.add(ticket)
			}
		}
		return usableTickets
	}

	fun isTicket(obj: TangibleObject): Boolean {
		return obj.ticketInformation != null
	}

	fun isTicketUsable(ticket: TangibleObject, nearest: TravelPoint?): Boolean {
		val ticketInformation = ticket.ticketInformation
		val departurePoint = ticketInformation.departurePoint
		val departureTerrain = ticketInformation.departurePlanet
		val currentTerrain = ticket.terrain

		return departureTerrain == currentTerrain && departurePoint == nearest!!.name
	}

	fun handleTicketUse(player: Player, ticket: TangibleObject, nearestPoint: TravelPoint, destinationPoint: TravelPoint) {
		val traveler = player.creatureObject
		if (!isTicket(ticket)) {
			Log.e("%s attempted to use an object that isn't a ticket!", player)
		} else if (nearestPoint.group!!.status != ShuttleStatus.GROUNDED) {
			val time = nearestPoint.group.getTimeRemaining()
			SystemMessageIntent(player, ProsePackage(StringId("travel/travel", "shuttle_board_delay"), "DI", time)).broadcast()
		} else if (!isTicketUsable(ticket, nearestPoint)) {
			// This ticket isn't valid for this point
			SystemMessageIntent(player, "@travel:wrong_shuttle").broadcast()
		} else if (nearestPoint.isWithinRange(player.creatureObject)) {
			// They can use their ticket if they're within range.
			Log.i("%s/%s is traveling from %s to %s", player.username, traveler.objectName, nearestPoint.name, destinationPoint)
			teleportAndDestroyTicket(destinationPoint, ticket, traveler)
		} else {
			SystemMessageIntent(player, "@travel:boarding_too_far").broadcast()
		}
	}

	private fun teleportAndDestroyTicket(destination: TravelPoint, ticket: SWGObject, traveler: CreatureObject) {
		DestroyObjectIntent(ticket).broadcast()
		traveler.moveToContainer(destination.collector!!.parent, destination.location)
	}

	private fun createGalaxyTravels() {
		val groundTime = config.getInt(this, "shuttleGroundTime", 120).toLong()
		val airTime = config.getInt(this, "shuttleAirTime", 60).toLong()

		createGalaxyTravel(SpecificObject.SO_TRANSPORT_SHUTTLE.template, 17, groundTime, airTime)
		createGalaxyTravel(SpecificObject.SO_TRANSPORT_STARPORT.template, 21, groundTime, airTime)
		createGalaxyTravel(SpecificObject.SO_TRANSPORT_STARPORT_THEED.template, 24, groundTime, airTime)
	}

	private fun createGalaxyTravel(template: String, landTime: Long, groundTime: Long, airTime: Long) {
		travel[template] = TravelGroup(landTime + 10, groundTime, airTime)
	}

	private fun loadTravelPoints() {
		try {
			SdbLoader.load(File("serverdata/travel/travel.sdb")).use { set ->
				while (set.next()) {
					loadTravelPoint(set)
				}
			}
		} catch (e: IOException) {
			Log.e("Failed to load a travel point")
			Log.e(e)
		}
	}

	private fun loadTravelPoint(set: SdbResultSet) {
		val pointName = set.getText("name")
		val x = set.getReal("x")
		val z = set.getReal("z")
		val type = set.getText("type")
		val travelPlanet = Terrain.getTerrainFromName(set.getText("planet"))
		if (travelPlanet == null) {
			Log.e("Invalid planet in travel.sdb: %s", set.getText("planet"))
			return
		}

		val group = getTravelGroupForType(type)
		val point = TravelPoint(pointName, Location(x, 0.0, z, travelPlanet), group, type.endsWith("starport"))
		group!!.addTravelPoint(point)
		pointContainer.addTravelPoint(point)
	}

	private fun getTravelGroupForType(type: String): TravelGroup? {
		when (type) {
			"shuttleport"    -> return getTravelGroup(SpecificObject.SO_TRANSPORT_SHUTTLE.template)
			"starport"       -> return getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT.template)
			"theed_starport" -> return getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT_THEED.template)
			else             -> {
				Log.w("Invalid travel point type: %s", type)
				return null
			}
		}
	}
}
