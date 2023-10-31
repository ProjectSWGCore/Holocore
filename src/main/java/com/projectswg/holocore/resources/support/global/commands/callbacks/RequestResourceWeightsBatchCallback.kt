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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.resource.ResourceWeight
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class RequestResourceWeightsBatchCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val clientCrc = args.trim().toInt()
		val resourceWeight = ResourceWeight(player.creatureObject.objectId)
		resourceWeight.attributes[0x00] = emptyList()
		resourceWeight.resourceMaxWeights[0x00] = emptyList()
		val serverCrc = 2027141215	// TODO don't hardcode slitherhorn CRCs
		val combinedCrc = 8706505225174593761L
		resourceWeight.schematicId = CRC.getCrc("object/draft_schematic/instrument/instrument_slitherhorn.iff")
		resourceWeight.schematicCrc = CRC.getCrc("object/draft_schematic/instrument/instrument_slitherhorn.iff")
		player.sendPacket(resourceWeight)
	}
}