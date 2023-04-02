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
package com.projectswg.holocore.services.support.data

import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.BasicLogStream
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.io.File

class PacketRecordingService : Service() {
	private val packetLogger: BasicLogStream = BasicLogStream(File("log/packets.txt"))

	@IntentHandler
	private fun handleInboundPacketIntent(ipi: InboundPacketIntent) {
		if (!packetDebug) return
		printPacketStream(true, ipi.player.networkId, ipi.packet.toString())
	}

	@IntentHandler
	private fun handleOutboundPacketIntent(opi: OutboundPacketIntent) {
		if (!packetDebug) return
		printPacketStream(false, opi.player.networkId, opi.packet.toString())
	}

	private fun printPacketStream(`in`: Boolean, networkId: Long, str: String) {
		packetLogger.log("%s %d:\t%s", if (`in`) "IN " else "OUT", networkId, str)
	}

	private val packetDebug: Boolean
		get() = config.getBoolean(this, "packetLogging", false)
}
