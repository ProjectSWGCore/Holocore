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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer

/**
 * Wraps the complex logic of the draft schematic CRCs.
 * Ensures they are readable by humans when persisted and work with the client.
 */
class DraftSchematicCombinedCrc : Encodable, MongoPersistable {

	var objectTemplate = ""
		set(value) {
			field = ClientFactory.formatToSharedFile(value)
		}

	override fun readMongo(data: MongoData) {
		objectTemplate = data.getString("objectTemplate", "")
	}

	override fun saveMongo(data: MongoData) {
		data.putString("objectTemplate", objectTemplate)
	}

	override fun decode(data: NetBuffer) {
		val combinedCrc = data.long
		val serverCrc = serverCrcFromCombinedCrc(combinedCrc)
		objectTemplate = CRC.getString(serverCrc)
	}

	override fun encode(): ByteArray {
		val buffer = NetBuffer.allocate(length)
		val serverCrc = getDraftSchematicServerCrc(objectTemplate)
		val clientCrc = getDraftSchematicClientCrc(objectTemplate)
		val combinedCrc = combinedCrc(serverCrc, clientCrc)
		buffer.addLong(combinedCrc)
		return buffer.array()
	}

	override val length = Long.SIZE_BYTES

	private fun combinedCrc(serverCrc: Int, clientCrc: Int): Long {
		return serverCrc.toLong() shl 32 and -0x100000000L or (clientCrc.toLong() and 0x00000000FFFFFFFFL)
	}

	private fun serverCrcFromCombinedCrc(combinedCrc: Long): Int {
		return (combinedCrc shr 32).toInt()
	}

	private fun getDraftSchematicServerCrc(schematicInGroupShared: String): Int {
		return CRC.getCrc(schematicInGroupShared)
	}

	private fun getDraftSchematicClientCrc(schematicInGroupShared: String): Int {
		val templateWithoutPrefix = schematicInGroupShared.replace("object/draft_schematic/", "")
		return CRC.getCrc(templateWithoutPrefix)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DraftSchematicCombinedCrc

		return objectTemplate == other.objectTemplate
	}

	override fun hashCode(): Int {
		return objectTemplate.hashCode()
	}

	override fun toString(): String {
		return "DraftSchematicCombinedCrc(objectTemplate='$objectTemplate')"
	}
	
}