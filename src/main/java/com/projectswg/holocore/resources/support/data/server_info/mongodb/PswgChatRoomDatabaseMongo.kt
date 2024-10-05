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

import com.mongodb.client.MongoCollection
import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.common.data.encodables.chat.ChatRoom
import com.projectswg.holocore.resources.support.data.server_info.database.PswgChatRoomDatabase
import org.bson.Document

class PswgChatRoomDatabaseMongo(private val collection: MongoCollection<Document>) : PswgChatRoomDatabase {
	override fun getChatRooms(): Collection<ChatRoom> {
		return collection.find().map { documentToChatRoom(it) }.toList()
	}

	private fun documentToChatRoom(it: Document): ChatRoom {
		val chatRoom = ChatRoom()
		chatRoom.id = it.getInteger("id")
		chatRoom.type = it.getInteger("type")
		chatRoom.isModerated = it.getBoolean("moderated")
		chatRoom.path = it.getString("path")
		chatRoom.owner = documentToChatAvatar(it.get("owner", Document::class.java))
		chatRoom.creator = documentToChatAvatar(it.get("creator", Document::class.java))
		chatRoom.title = it.getString("title")
		it.getList("moderators", Document::class.java, emptyList()).map { documentToChatAvatar(it) }.forEach { chatRoom.addModerator(it) }
		it.getList("invited", Document::class.java, emptyList()).map { documentToChatAvatar(it) }.forEach { chatRoom.addInvited(it) }
		it.getList("banned", Document::class.java, emptyList()).map { documentToChatAvatar(it) }.forEach { chatRoom.addBanned(it) }
		return chatRoom
	}

	private fun documentToChatAvatar(document: Document): ChatAvatar {
		return ChatAvatar(document.getString("name"))
	}

	override fun addChatRoom(chatRoom: ChatRoom) {
		collection.insertOne(chatRoomToDocument(chatRoom))
	}

	private fun chatRoomToDocument(chatRoom: ChatRoom): Document {
		val document = Document()
		document["id"] = chatRoom.id
		document["type"] = chatRoom.type
		document["moderated"] = chatRoom.isModerated
		document["path"] = chatRoom.path
		document["owner"] = chatAvatarToDocument(chatRoom.owner)
		document["creator"] = chatAvatarToDocument(chatRoom.creator)
		document["title"] = chatRoom.title
		document["moderators"] = chatRoom.moderators.map { chatAvatarToDocument(it) }
		document["invited"] = chatRoom.invited.map { chatAvatarToDocument(it) }
		document["banned"] = chatRoom.banned.map { chatAvatarToDocument(it) }
		return document
	}

	private fun chatAvatarToDocument(chatAvatar: ChatAvatar): Document {
		val document = Document()
		document["name"] = chatAvatar.name
		return document
	}
}