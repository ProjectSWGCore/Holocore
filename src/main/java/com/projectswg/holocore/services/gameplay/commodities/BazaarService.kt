/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.commodities

import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage
import com.projectswg.common.data.encodables.player.Mail
import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.auction.*
import com.projectswg.common.network.packets.swg.zone.auction.IsVendorOwnerResponseMessage.VendorOwnerResult
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.support.global.chat.PersistentMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarAvailableItemsDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarInstantSalesDatabase
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class BazaarService : Service() {

	@IntentHandler
	private fun handleInboundPacket(inboundPacketIntent: InboundPacketIntent) {
		val packet = inboundPacketIntent.packet
		val player = inboundPacketIntent.player

		when (packet) {
			is RetrieveAuctionItemMessage     -> handleRetrieveAuctionItemMessage(packet, player)
			is BidAuctionMessage              -> handleBidAuctionMessage(packet, player)
			is CommoditiesItemTypeListRequest -> handleCommoditiesItemTypeListRequest(packet, player)
			is AuctionQueryHeadersMessage     -> handleAuctionQueryHeadersMessage(packet, player)
			is IsVendorOwnerMessage           -> handleIsVendorOwnerMessage(packet, player)
			is CreateImmediateAuctionMessage  -> handleCreateImmediateAuctionMessage(packet, player)
			is GetAuctionDetails              -> handleGetAuctionDetails(packet, player)
		}
	}

	private fun handleRetrieveAuctionItemMessage(packet: RetrieveAuctionItemMessage, player: Player) {
		val objectId = packet.objectId
		val objectById = ObjectLookup.getObjectById(objectId) ?: return
		val availableItem = PswgDatabase.bazaarAvailableItems.getAvailableItem(objectId) ?: return
		val container = getContainer(buyer = player.creatureObject, objectById) ?: return

		if (player.creatureObject.objectId != availableItem.ownerId) {
			player.sendPacket(RetrieveAuctionItemResponseMessage(objectId = objectId, error = 1))
			return
		}

		if (!isSpaceLeftInContainer(container = container, objectById = objectById)) {
			player.sendPacket(RetrieveAuctionItemResponseMessage(objectId = objectId, error = 12))
			return
		}

		PswgDatabase.bazaarAvailableItems.removeAvailableItem(availableItem)
		objectById.moveToContainer(container)
		player.sendPacket(RetrieveAuctionItemResponseMessage(objectId = objectId, error = 0))
		StandardLog.onPlayerEvent(this, player, "retrieved item %s", availableItem)
	}

	private fun handleBidAuctionMessage(packet: BidAuctionMessage, player: Player) {
		val objectId = packet.objectId
		val instantSaleItem = PswgDatabase.bazaarInstantSales.getInstantSaleItem(objectId) ?: return
		val objectById = ObjectLookup.getObjectById(objectId) ?: return
		val seller = ObjectLookup.getObjectById(instantSaleItem.ownerId) as CreatureObject? ?: return
		val bazaarObject = ObjectLookup.getObjectById(instantSaleItem.bazaarObjectId) ?: return
		val buyer = player.creatureObject
		val container = getContainer(buyer = buyer, objectById = objectById) ?: return

		if (!buyer.removeFromBankAndCash(instantSaleItem.price.toLong())) {
			player.sendPacket(BidAuctionResponseMessage(objectId = objectId, error = 9))
			return
		}

		PswgDatabase.bazaarInstantSales.removeInstantSaleItem(instantSaleItem)

		if (isSpaceLeftInContainer(container = container, objectById = objectById)) {
			objectById.moveToContainer(container)
		} else {
			PswgDatabase.bazaarAvailableItems.addAvailableItem(
				PswgBazaarAvailableItemsDatabase.AvailableItemMetadata(
					itemObjectId = instantSaleItem.itemObjectId,
					price = instantSaleItem.price,
					expiresAt = LocalDateTime.now().plusDays(30),
					description = instantSaleItem.description,
					ownerId = buyer.objectId,
					bazaarObjectId = instantSaleItem.bazaarObjectId,
					saleType = PswgBazaarAvailableItemsDatabase.AvailableItemSaleType.INSTANT
				)
			)
		}

		seller.addToBank(instantSaleItem.price.toLong())
		sendMails(bazaarObject = bazaarObject, objectById = objectById, buyer = buyer, seller = seller, instantSaleItem = instantSaleItem)
		player.sendPacket(BidAuctionResponseMessage(objectId = objectId, error = 0))
		StandardLog.onPlayerEvent(this, player, "bought %s", instantSaleItem)
	}

	private fun isSpaceLeftInContainer(container: SWGObject, objectById: SWGObject) = (container.containedObjects.size + objectById.volume) <= container.maxContainerSize

	private fun getContainer(buyer: CreatureObject, objectById: SWGObject): SWGObject? {
		return when (objectById) {
			is TangibleObject   -> buyer.inventory
			is IntangibleObject -> buyer.datapad
			else                -> null
		}
	}

	private fun sendMails(bazaarObject: SWGObject, objectById: SWGObject, buyer: CreatureObject, seller: CreatureObject, instantSaleItem: PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata) {
		val terminalName = getDisplayName(bazaarObject)
		val currentCity = getCurrentCity(bazaarObject)
		val sender = "SWG." + ProjectSWG.getGalaxy().name + ".auctioner"
		val itemName = getDisplayName(objectById)
		val location = bazaarObject.location
		sendMailToSeller(
			sender = sender,
			buyer = buyer,
			seller = seller,
			itemName = itemName,
			price = instantSaleItem.price,
			currentCity = currentCity,
			location = location
		)
		sendMailToBuyer(
			sender = sender,
			buyer = buyer,
			seller = seller,
			itemName = itemName,
			price = instantSaleItem.price,
			currentCity = currentCity,
			location = location,
			terminalName = terminalName
		)
	}

	private fun sendMailToSeller(sender: String, buyer: CreatureObject, seller: CreatureObject, itemName: String, price: Int, currentCity: String, location: Location) {
		val terrain = location.terrain
		val mail = Mail(sender, "@auction:subject_instant_seller", "", seller.objectId)
		mail.outOfBandPackage = OutOfBandPackage(
			ProsePackage(StringId("auction", "seller_success"), "TO", itemName, "TT", buyer.objectName, "DI", price),
			ProsePackage(StringId("auction", "seller_success_location"), "TT", currentCity, "TO", "@planet_n:${terrain.getName()}")
		)

		PersistentMessageIntent(seller, mail, ProjectSWG.getGalaxy().name).broadcast()
	}

	private fun sendMailToBuyer(sender: String, buyer: CreatureObject, seller: CreatureObject, itemName: String, price: Int, currentCity: String, location: Location, terminalName: String) {
		val terrain = location.terrain
		val mail = Mail(sender, "@auction:subject_instant_buyer", "", buyer.objectId)
		val waypoint = WaypointPackage()
		waypoint.terrain = terrain
		waypoint.position = location.position
		waypoint.name = terminalName
		waypoint.isActive = false
		mail.outOfBandPackage = OutOfBandPackage(
			ProsePackage(StringId("auction", "buyer_success"), "TO", itemName, "TT", seller.objectName, "DI", price),
			ProsePackage(StringId("auction", "buyer_success_location"), "TT", currentCity, "TO", "@planet_n:${terrain.getName()}"),
			waypoint
		)

		PersistentMessageIntent(buyer, mail, ProjectSWG.getGalaxy().name).broadcast()
	}

	private fun handleGetAuctionDetails(packet: GetAuctionDetails, player: Player) {
		val objectById = ObjectLookup.getObjectById(packet.objectId) as TangibleObject
		val properties = objectById.getAttributeList(player.creatureObject).asMap()
		val itemDescription = getItemDescription(objectById)
		player.sendPacket(GetAuctionDetailsResponse(objectById.objectId, properties, itemDescription, objectById.template, objectById.appearanceData))
	}

	private fun getItemDescription(objectById: TangibleObject): String {
		val instantSaleItem = PswgDatabase.bazaarInstantSales.getInstantSaleItem(objectById.objectId)
		return instantSaleItem?.description ?: ""
	}

	private fun handleCreateImmediateAuctionMessage(packet: CreateImmediateAuctionMessage, player: Player) {
		val objectId = packet.objectId
		val objectById = ObjectLookup.getObjectById(objectId) ?: return

		if (objectById.owner != player) {
			return
		}

		if (objectById.isNoTrade) {
			player.sendPacket(
				CreateAuctionResponseMessage(
					objectId = objectId, vendorId = packet.vendorId, status = 21
				)
			)

			return
		}

		if (objectById.owner != player) {
			player.sendPacket(
				CreateAuctionResponseMessage(
					objectId = objectId, vendorId = packet.vendorId, status = 8
				)
			)

			return
		}

		objectById.moveToContainer(null, Location.zero())
		if (player.creatureObject.equippedWeapon == objectById) {
			player.creatureObject.equippedWeapon = null
		}

		val expiresAt = LocalDateTime.now().plusDays(7L)
		PswgDatabase.bazaarInstantSales.addInstantSaleItem(
			PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata(
				itemObjectId = objectId,
				price = packet.price,
				expiresAt = expiresAt,
				description = packet.description,
				ownerId = player.creatureObject.objectId,
				bazaarObjectId = packet.vendorId
			)
		)

		StandardLog.onPlayerEvent(this, player, "listed %s for instant sale with price of %d", objectById, packet.price)

		player.sendPacket(
			CreateAuctionResponseMessage(
				objectId = objectId, vendorId = packet.vendorId, status = 0
			)
		)
	}

	private fun handleAuctionQueryHeadersMessage(packet: AuctionQueryHeadersMessage, player: Player) {
		// 2 All Auctions
		// 3 My Sales
		// 4 My bids
		// 5 Available Items
		// 6 Offers
		// 7 For Sale (vendor) / Vendor Locations (bazaar)
		// 8 Stockroom
		// 9 Offers to Vendor
		when (packet.windowType) {
			2 -> handleAllAuctionsWindow(packet, player)
			3 -> handleMySalesWindow(packet, player)
			5 -> handleAvailableItemsWindow(packet, player)
		}
	}

	private fun handleAvailableItemsWindow(packet: AuctionQueryHeadersMessage, player: Player) {
		val availableItems = PswgDatabase.bazaarAvailableItems.getMyAvailableItems(player.creatureObject.objectId)
		val now = LocalDateTime.now()
		handleItemWindow(packet, player, availableItems.mapNotNull { availbleItemToAuctionItem(now, it) })
	}

	private fun handleMySalesWindow(packet: AuctionQueryHeadersMessage, player: Player) {
		val instantSaleItems = PswgDatabase.bazaarInstantSales.getMyInstantSaleItems(player.creatureObject.objectId)
		val now = LocalDateTime.now()
		handleItemWindow(packet, player, instantSaleItems.mapNotNull { instantSaleItemToAuctionItem(now, it) })
	}

	private fun handleItemWindow(packet: AuctionQueryHeadersMessage, player: Player, auctionItems: List<AuctionQueryHeadersResponseMessage.AuctionItem>) {
		val auctionQueryHeadersResponseMessage = AuctionQueryHeadersResponseMessage()
		auctionQueryHeadersResponseMessage.updateCounter = packet.updateCounter
		auctionQueryHeadersResponseMessage.windowType = packet.windowType
		auctionItems.filter { it.expireTime >= 60 }.forEach { auctionQueryHeadersResponseMessage.addItem(it) }
		player.sendPacket(auctionQueryHeadersResponseMessage)
	}

	private fun instantSaleItemToAuctionItem(now: LocalDateTime, instantSaleItem: PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata): AuctionQueryHeadersResponseMessage.AuctionItem? {
		val expiresInSeconds = now.until(instantSaleItem.expiresAt, SECONDS).toInt()
		val objectById = ObjectLookup.getObjectById(instantSaleItem.itemObjectId) ?: return null
		val itemOwner = ObjectLookup.getObjectById(instantSaleItem.ownerId) ?: return null
		val bazaarTerminal = ObjectLookup.getObjectById(instantSaleItem.bazaarObjectId) ?: return null
		val auctionItem = AuctionQueryHeadersResponseMessage.AuctionItem()
		auctionItem.objectId = objectById.objectId
		auctionItem.isInstant = true
		auctionItem.itemDescription = instantSaleItem.description
		auctionItem.price = instantSaleItem.price
		auctionItem.expireTime = expiresInSeconds
		auctionItem.itemName = getDisplayName(objectById)
		auctionItem.ownerId = itemOwner.objectId
		auctionItem.ownerName = itemOwner.objectName
		auctionItem.vuid = getMarketName(bazaarTerminal)
		auctionItem.bidderName = ""
		auctionItem.itemType = objectById.gameObjectType.typeMask
		auctionItem.status = AuctionQueryHeadersResponseMessage.AuctionState.FORSALE
		return auctionItem
	}

	private fun availbleItemToAuctionItem(now: LocalDateTime, availableItem: PswgBazaarAvailableItemsDatabase.AvailableItemMetadata): AuctionQueryHeadersResponseMessage.AuctionItem? {
		val expiresInSeconds = now.until(availableItem.expiresAt, SECONDS).toInt()
		val objectById = ObjectLookup.getObjectById(availableItem.itemObjectId) ?: return null
		val itemOwner = ObjectLookup.getObjectById(availableItem.ownerId) ?: return null
		val bazaarTerminal = ObjectLookup.getObjectById(availableItem.bazaarObjectId) ?: return null
		val auctionItem = AuctionQueryHeadersResponseMessage.AuctionItem()
		auctionItem.objectId = objectById.objectId
		auctionItem.isInstant = availableItem.saleType == PswgBazaarAvailableItemsDatabase.AvailableItemSaleType.INSTANT
		auctionItem.itemDescription = availableItem.description
		auctionItem.price = availableItem.price
		auctionItem.expireTime = expiresInSeconds
		auctionItem.itemName = getDisplayName(objectById)
		auctionItem.ownerId = itemOwner.objectId
		auctionItem.ownerName = itemOwner.objectName
		auctionItem.vuid = getMarketName(bazaarTerminal)
		auctionItem.bidderName = ""
		auctionItem.itemType = objectById.gameObjectType.typeMask
		auctionItem.status = AuctionQueryHeadersResponseMessage.AuctionState.WITHDRAW
		return auctionItem
	}

	private fun handleAllAuctionsWindow(packet: AuctionQueryHeadersMessage, player: Player) {
		val instantSaleItems = PswgDatabase.bazaarInstantSales.getInstantSaleItems()
		val now = LocalDateTime.now()
		handleItemWindow(packet, player, instantSaleItems.mapNotNull { instantSaleItemToAuctionItem(now, it) })
	}

	private fun getDisplayName(objectById: SWGObject): String {
		val itemName = objectById.objectName

		if (itemName.isNotBlank()) {
			return itemName
		}

		return objectById.stringId.toString()
	}

	private fun handleIsVendorOwnerMessage(packet: IsVendorOwnerMessage, player: Player) {
		val ownerResult = VendorOwnerResult.IS_BAZAAR.id
		val terminalId = packet.terminalId
		val terminal = ObjectLookup.getObjectById(terminalId) ?: return
		val marketName = getMarketName(terminal)
		player.sendPacket(IsVendorOwnerResponseMessage(terminalId, marketName, 0, ownerResult, 64))
	}

	private fun getMarketName(terminal: SWGObject): String {
		val terminalName = "@terminal_name:terminal_bazaar"
		val x = terminal.x.toInt()
		val z = terminal.z.toInt()
		val terminalId = terminal.objectId
		val currentCity = getCurrentCity(terminal)
		val terrain = terminal.terrain.getName()
		return "$terrain.$currentCity.$terminalName.$terminalId#$x,$z"
	}

	private fun getCurrentCity(terminal: SWGObject): String {
		return ServerData.staticCities.getCities(terminal.terrain).first { it.isWithinRange(terminal) }.name
	}

	private fun handleCommoditiesItemTypeListRequest(packet: CommoditiesItemTypeListRequest, player: Player) {
		player.sendPacket(CommoditiesItemTypeListResponse(ProjectSWG.getGalaxy().name, 0, 0, 0, "test category name", 0, "test type"))
	}
}