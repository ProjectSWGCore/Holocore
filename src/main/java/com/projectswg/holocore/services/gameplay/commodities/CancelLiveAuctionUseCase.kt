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

import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarAvailableItemsDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarInstantSalesDatabase
import java.time.LocalDateTime

class CancelLiveAuctionUseCase(private val bazaarInstantSalesDatabase: PswgBazaarInstantSalesDatabase, private val bazaarAvailableItemsDatabase: PswgBazaarAvailableItemsDatabase) {

	fun cancelLiveAuction(itemObjectId: Long, creatureObjectId: Long): CancelLiveAuctionResult {
		val instantSaleItem = bazaarInstantSalesDatabase.getInstantSaleItem(itemObjectId) ?: return CancelLiveAuctionResult.NOT_FOUND

		if (instantSaleItem.ownerId != creatureObjectId) {
			return CancelLiveAuctionResult.NOT_OWNER
		}

		bazaarInstantSalesDatabase.removeInstantSaleItem(instantSaleItem)
		bazaarAvailableItemsDatabase.addAvailableItem(
			PswgBazaarAvailableItemsDatabase.AvailableItemMetadata(
				itemObjectId = instantSaleItem.itemObjectId,
				price = instantSaleItem.price,
				expiresAt = LocalDateTime.now().plusDays(30),
				description = instantSaleItem.description,
				ownerId = creatureObjectId,
				instantSaleItem.bazaarObjectId,
				PswgBazaarAvailableItemsDatabase.AvailableItemSaleType.INSTANT
			)
		)
		
		return CancelLiveAuctionResult.SUCCESS
	}
}

enum class CancelLiveAuctionResult {
	NOT_FOUND, NOT_OWNER, SUCCESS
}