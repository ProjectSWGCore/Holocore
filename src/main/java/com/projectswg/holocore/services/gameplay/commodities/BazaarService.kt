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

import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.auction.*
import com.projectswg.common.network.packets.swg.zone.auction.IsVendorOwnerResponseMessage.VendorOwnerResult
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarInstantSalesDatabase
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
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
			is CommoditiesItemTypeListRequest -> handleCommoditiesItemTypeListRequest(packet, player)
			is AuctionQueryHeadersMessage     -> handleAuctionQueryHeadersMessage(packet, player)
			is IsVendorOwnerMessage           -> handleIsVendorOwnerMessage(packet, player)
			is CreateImmediateAuctionMessage  -> handleCreateImmediateAuctionMessage(packet, player)
			is GetAuctionDetails              -> handleGetAuctionDetails(packet, player)
		}
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
		val windowType = packet.windowType

		if (windowType == 2) {
			handleAllAuctionsWindow(packet, player)
		} else if (windowType == 3) {
			handleMySalesWindow(packet, player)
		}
	}

	private fun handleMySalesWindow(packet: AuctionQueryHeadersMessage, player: Player) {
		val instantSaleItems = PswgDatabase.bazaarInstantSales.getMyInstantSaleItems(player.creatureObject.objectId)
		handleItemWindow(packet, instantSaleItems, player)
	}

	private fun handleItemWindow(packet: AuctionQueryHeadersMessage, instantSaleItems: Collection<PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata>, player: Player) {
		val auctionQueryHeadersResponseMessage = AuctionQueryHeadersResponseMessage()
		auctionQueryHeadersResponseMessage.updateCounter = packet.updateCounter
		auctionQueryHeadersResponseMessage.windowType = packet.windowType
		val now = LocalDateTime.now()

		for (instantSaleItem in instantSaleItems) {
			val expiresInSeconds = now.until(instantSaleItem.expiresAt, SECONDS).toInt()
			if (expiresInSeconds < 60) {    // Client crashes if this ever goes below 60
				continue
			}

			val objectById = ObjectLookup.getObjectById(instantSaleItem.itemObjectId)
			val itemOwner = ObjectLookup.getObjectById(instantSaleItem.ownerId)

			if (objectById != null && itemOwner != null) {
				val auctionItem = toAuctionItem(instantSaleItem, expiresInSeconds, objectById, itemOwner)

				if (auctionItem != null) {
					auctionQueryHeadersResponseMessage.addItem(auctionItem)
				}
			}
		}

		player.sendPacket(auctionQueryHeadersResponseMessage)
	}

	private fun toAuctionItem(instantSaleItem: PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata, expiresInSeconds: Int, objectById: SWGObject, itemOwner: SWGObject): AuctionQueryHeadersResponseMessage.AuctionItem? {
		val auctionItem = AuctionQueryHeadersResponseMessage.AuctionItem()
		val bazaarTerminal = ObjectLookup.getObjectById(instantSaleItem.bazaarObjectId) ?: return null
		auctionItem.objectId = objectById.objectId
		auctionItem.isInstant = true
		auctionItem.itemDescription = instantSaleItem.description
		auctionItem.price = instantSaleItem.price
		auctionItem.expireTime = expiresInSeconds
		auctionItem.itemName = getItemName(objectById)
		auctionItem.ownerId = itemOwner.objectId
		auctionItem.ownerName = itemOwner.objectName
		auctionItem.vuid = getMarketName(bazaarTerminal)
		auctionItem.bidderName = ""
		auctionItem.itemType = objectById.gameObjectType.typeMask
		auctionItem.status = AuctionQueryHeadersResponseMessage.AuctionState.FORSALE
		return auctionItem
	}

	private fun handleAllAuctionsWindow(packet: AuctionQueryHeadersMessage, player: Player) {
		val instantSaleItems = PswgDatabase.bazaarInstantSales.getInstantSaleItems()
		handleItemWindow(packet, instantSaleItems, player)
	}

	private fun getItemName(objectById: SWGObject): String? {
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
		val currentCity = ServerData.staticCities.getCities(terminal.terrain).first { it.isWithinRange(terminal) }.name
		val terrain = terminal.terrain.getName()
		return "$terrain.$currentCity.$terminalName.$terminalId#$x,$z"
	}

	private fun handleCommoditiesItemTypeListRequest(packet: CommoditiesItemTypeListRequest, player: Player) {
		player.sendPacket(CommoditiesItemTypeListResponse(ProjectSWG.getGalaxy().name, 0, 0, 0, "test category name", 0, "test type"))
	}
}