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
package com.projectswg.holocore.headless

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.server_ui.SuiCreatePageMessage
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.concurrent.TimeUnit

fun ZonedInCharacter.tipCash(target: SWGObject, amount: Int) {
	sendCommand("tip", target, amount.toString())
	val packet = player.waitForNextPacket(ChatSystemMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: throw IllegalStateException("No known packet received")
	checkSystemMessage(packet)
}

fun ZonedInCharacter.tipBank(target: SWGObject, amount: Int): SuiWindow {
	sendCommand("tip", target, "$amount bank")
	val packet = player.waitForNextPacket(setOf(SuiCreatePageMessage::class.java, ChatSystemMessage::class.java), 50, TimeUnit.MILLISECONDS) ?: throw IllegalStateException("No known packet received")

	if (packet is SuiCreatePageMessage) {
		val suiWindowId = packet.window.id

		return SuiWindow(player, suiWindowId)
	} else if (packet is ChatSystemMessage) {
		checkSystemMessage(packet)
	}
	
	throw IllegalStateException()
}

private fun checkSystemMessage(packet: ChatSystemMessage) {
	val oob = packet.oob

	if (oob != null) {
		val firstOobPackage = oob.packages.first()
		if (firstOobPackage is ProsePackage) {
			val key = firstOobPackage.base.key

			if (key != "prose_tip_pass_self") {
				throw TipException(firstOobPackage.toString())
			}
		}
	}

	val message = packet.message

	if (message != null && message.contains("cannot")) {
		throw TipException(message)
	}
}

