/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.common.data.encodables.chat.ChatRoom
import com.projectswg.holocore.resources.support.data.server_info.database.PswgChatRoomDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PswgChatRoomDatabaseMongoTest {
	private lateinit var database: MongoDatabase

	@BeforeEach
	fun setUp() {
		database = MongoDBTestContainer.mongoClient.getDatabase("cu")
	}

	@AfterEach
	fun tearDown() {
		database.drop()
	}

	private val chatRooms: PswgChatRoomDatabase
		get() {
			return PswgChatRoomDatabaseMongo(database.getCollection("chatRooms"))
		}

	@Test
	fun `chat rooms can be added`() {
		chatRooms.addChatRoom(exampleChatRoom())

		val collection = database.getCollection("chatRooms")
		val countDocuments = collection.countDocuments()

		assertEquals(1, countDocuments)
	}
	
	@Test
	fun `chat rooms can be retrieved`() {
		val added = exampleChatRoom()
		chatRooms.addChatRoom(added)

		val retrieved = chatRooms.getChatRooms().first()

		assertEquals(added, retrieved)
	}

	private fun exampleChatRoom(): ChatRoom {
		val chatRoom = ChatRoom()
		chatRoom.id = 3
		chatRoom.type = 1
		chatRoom.isModerated = true
		chatRoom.path = "SWG.Holocore.BestChatRoom"
		chatRoom.owner = ChatAvatar("Test User 1")
		chatRoom.creator = ChatAvatar("Test User 2")
		chatRoom.title = "BestChatRoom"
		chatRoom.addModerator(ChatAvatar("Test User 3"))
		chatRoom.addInvited(ChatAvatar("Test User 4"))
		chatRoom.addBanned(ChatAvatar("Test User 5"))
		return chatRoom
	}
}