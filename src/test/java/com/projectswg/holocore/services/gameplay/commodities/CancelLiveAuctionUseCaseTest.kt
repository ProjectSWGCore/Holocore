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

import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarInstantSalesDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month

class CancelLiveAuctionUseCaseTest {
	
	private val bazaarInstantSalesDatabase = MemoryBazaarInstantSalesDatabase()
	private val bazaarAvailableItemsDatabase = MemoryBazaarAvailableItemsDatabase()
	private val cancelLiveAuctionUseCase = CancelLiveAuctionUseCase(bazaarInstantSalesDatabase, bazaarAvailableItemsDatabase)

	@Test
	fun `NOT_FOUND when canceling sale of an item that does not exist`() {
		val result = cancelLiveAuctionUseCase.cancelLiveAuction(1234, 2)

		assertEquals(CancelLiveAuctionResult.NOT_FOUND, result)
	}

	@Test
	fun `NOT_OWNER when canceling sale of an item that is owned by someone else`() {
		bazaarInstantSalesDatabase.addInstantSaleItem(
			PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata(
				itemObjectId = 1234,
				price = 700,
				expiresAt = LocalDateTime.of(2023, Month.AUGUST, 18, 3, 55),
				description = "Test item",
				ownerId = 3,
				bazaarObjectId = 1
			)
		)
		
		val result = cancelLiveAuctionUseCase.cancelLiveAuction(1234, 2)

		assertEquals(CancelLiveAuctionResult.NOT_OWNER, result)
	}

	@Test
	fun `SUCCESS when canceling sale of an item that is owned by you`() {
		bazaarInstantSalesDatabase.addInstantSaleItem(
			PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata(
				itemObjectId = 1234,
				price = 700,
				expiresAt = LocalDateTime.of(2023, Month.AUGUST, 18, 3, 55),
				description = "Test item",
				ownerId = 2,
				bazaarObjectId = 1
			)
		)

		val result = cancelLiveAuctionUseCase.cancelLiveAuction(1234, 2)

		assertEquals(CancelLiveAuctionResult.SUCCESS, result)
	}

	@Test
	fun `canceled items are stored as available items`() {
		bazaarInstantSalesDatabase.addInstantSaleItem(
			PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata(
				itemObjectId = 1234,
				price = 700,
				expiresAt = LocalDateTime.of(2023, Month.AUGUST, 18, 3, 55),
				description = "Test item",
				ownerId = 2,
				bazaarObjectId = 1
			)
		)
		val bazaarAvailableItemsDatabase = bazaarAvailableItemsDatabase
		val cancelLiveAuctionUseCase = CancelLiveAuctionUseCase(bazaarInstantSalesDatabase, bazaarAvailableItemsDatabase)

		cancelLiveAuctionUseCase.cancelLiveAuction(1234, 2)

		val myAvailableItems = bazaarAvailableItemsDatabase.getMyAvailableItems(2)
		assertEquals(1, myAvailableItems.size)
	}

	@Test
	fun `canceled items are removed from active listings`() {
		bazaarInstantSalesDatabase.addInstantSaleItem(
			PswgBazaarInstantSalesDatabase.InstantSaleItemMetadata(
				itemObjectId = 1234,
				price = 700,
				expiresAt = LocalDateTime.of(2023, Month.AUGUST, 18, 3, 55),
				description = "Test item",
				ownerId = 2,
				bazaarObjectId = 1
			)
		)
		val bazaarAvailableItemsDatabase = bazaarAvailableItemsDatabase
		val cancelLiveAuctionUseCase = CancelLiveAuctionUseCase(bazaarInstantSalesDatabase, bazaarAvailableItemsDatabase)

		cancelLiveAuctionUseCase.cancelLiveAuction(1234, 2)

		val myActiveListings = bazaarInstantSalesDatabase.getMyInstantSaleItems(2)
		assertEquals(0, myActiveListings.size)
	}
}
