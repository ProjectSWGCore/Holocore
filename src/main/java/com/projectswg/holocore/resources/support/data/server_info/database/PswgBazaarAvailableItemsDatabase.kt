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
package com.projectswg.holocore.resources.support.data.server_info.database

import java.time.LocalDateTime

interface PswgBazaarAvailableItemsDatabase {
	fun getAvailableItem(itemObjectId: Long): AvailableItemMetadata?
	fun addAvailableItem(availableItemMetadata: AvailableItemMetadata)
	fun getMyAvailableItems(ownerId: Long): Collection<AvailableItemMetadata>
	fun removeAvailableItem(availableItemMetadata: AvailableItemMetadata)

	data class AvailableItemMetadata(val itemObjectId: Long, val price: Int, val expiresAt: LocalDateTime, val description: String, val ownerId: Long, val bazaarObjectId: Long, val saleType: AvailableItemSaleType)

	enum class AvailableItemSaleType {
		INSTANT, AUCTION
	}

	companion object {

		fun createDefault(): PswgBazaarAvailableItemsDatabase {
			return object : PswgBazaarAvailableItemsDatabase {

				private val availableItems = mutableListOf<AvailableItemMetadata>()

				override fun getAvailableItem(itemObjectId: Long): AvailableItemMetadata? {
					return availableItems.firstOrNull { it.itemObjectId == itemObjectId }
				}

				override fun addAvailableItem(availableItemMetadata: AvailableItemMetadata) {
					availableItems.add(availableItemMetadata)
				}

				override fun getMyAvailableItems(ownerId: Long): Collection<AvailableItemMetadata> {
					return availableItems.filter { it.ownerId == ownerId }
				}

				override fun removeAvailableItem(availableItemMetadata: AvailableItemMetadata) {
					availableItems.remove(availableItemMetadata)
				}
			}
		}

	}
}