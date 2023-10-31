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

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.network.NetBuffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DraftSchematicCombinedCrcTest {
	
	@Test
	fun restoreFromPersistenceLayer() {
		val draftSchematicCombinedCrc = DraftSchematicCombinedCrc()
		val mongoData = MongoData()
		mongoData.putString("objectTemplate", "object/draft_schematic/weapon/shared_carbine_blaster_cdef.iff")
		
		draftSchematicCombinedCrc.readMongo(mongoData)
		
		assertEquals(draftSchematicCombinedCrc.objectTemplate, "object/draft_schematic/weapon/shared_carbine_blaster_cdef.iff")
	}

	@Test
	fun saveToPersistenceLayer() {
		val draftSchematicCombinedCrc = DraftSchematicCombinedCrc()
		draftSchematicCombinedCrc.objectTemplate = "object/draft_schematic/weapon/carbine_blaster_cdef.iff"
		val mongoData = MongoData()

		draftSchematicCombinedCrc.saveMongo(mongoData)

		assertEquals(mongoData.getString("objectTemplate"), "object/draft_schematic/weapon/shared_carbine_blaster_cdef.iff")
	}

	@Test
	fun packetEncode() {
		val draftSchematicCombinedCrc = DraftSchematicCombinedCrc()
		draftSchematicCombinedCrc.objectTemplate = "object/draft_schematic/instrument/instrument_slitherhorn.iff"

		val encode = draftSchematicCombinedCrc.encode()

		val buffer = NetBuffer.wrap(encode)
		assertEquals(buffer.long, 8706505225174593761)
	}

	@Test
	fun packetDecode() {
		val draftSchematicCombinedCrc = DraftSchematicCombinedCrc()
		val buffer = NetBuffer.allocate(8)
		buffer.addLong(8706505225174593761)
		buffer.rewind()
		
		draftSchematicCombinedCrc.decode(buffer)
		
		assertEquals(draftSchematicCombinedCrc.objectTemplate, "object/draft_schematic/instrument/shared_instrument_slitherhorn.iff")
	}
}