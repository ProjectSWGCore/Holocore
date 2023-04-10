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
package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarAvailableItemsDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month

class PswgBazaarAvailableItemsDatabaseMongoTest {
	
	private lateinit var database: MongoDatabase

	@BeforeEach
	fun setUp() {
		database = MongoDBTestContainer.mongoClient.getDatabase("cu")
	}

	@AfterEach
	fun tearDown() {
		database.drop()
	}

	private val bazaarItems: PswgBazaarAvailableItemsDatabase
		get() {
			return PswgBazaarAvailableItemsDatabaseMongo(database.getCollection("bazaarAvailableItems"))
		}

	@Test
	fun `specific item is null if it doesn't exist`() {
		bazaarItems.addAvailableItem(exampleItem())

		val retrieved = bazaarItems.getAvailableItem(2L)

		assertNull(retrieved)
	}
	
	@Test
	fun `items belonging to a specific owner can be retrieved`() {
		val added = exampleItem()
		bazaarItems.addAvailableItem(added)

		val retrieved = bazaarItems.getMyAvailableItems(3L).first()

		assertEquals(added, retrieved)
	}
	
	@Test
	fun `items belonging to a specific owner is an empty collection if there are no items`() {
		bazaarItems.addAvailableItem(exampleItem())

		val retrieved = bazaarItems.getMyAvailableItems(4L).size

		assertEquals(0, retrieved)
	}

	@Test
	fun `items can be removed`() {
		val instantSaleItemMetadata = exampleItem()
		bazaarItems.addAvailableItem(instantSaleItemMetadata)

		bazaarItems.removeAvailableItem(instantSaleItemMetadata)

		val collection = database.getCollection("bazaarAvailableItems")
		val countDocuments = collection.countDocuments()
		assertEquals(0, countDocuments)
	}

	private fun exampleItem() = PswgBazaarAvailableItemsDatabase.AvailableItemMetadata(
		itemObjectId = 1L,
		price = 1337,
		expiresAt = LocalDateTime.of(2023, Month.APRIL, 7, 10, 1, 30),
		description = "This is a great item",
		bazaarObjectId = 2L,
		ownerId = 3L,
		saleType = PswgBazaarAvailableItemsDatabase.AvailableItemSaleType.INSTANT
	)
}