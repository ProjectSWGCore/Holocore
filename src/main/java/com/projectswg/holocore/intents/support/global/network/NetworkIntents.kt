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
package com.projectswg.holocore.intents.support.global.network

import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped
import com.projectswg.holocore.resources.support.global.network.DisconnectReason
import com.projectswg.holocore.resources.support.global.network.NetworkClient
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.control.Intent

data class OutboundPacketIntent(val player: Player, val packet: SWGPacket) : Intent()
data class InboundPacketPendingIntent(val client: NetworkClient) : Intent()
data class InboundPacketIntent(val player: Player, val packet: SWGPacket) : Intent()
data class ForceLogoutIntent(val player: Player) : Intent()
data class ConnectionOpenedIntent(val player: Player) : Intent()
data class ConnectionClosedIntent(val player: Player, val reason: HoloConnectionStopped.ConnectionStoppedReason) : Intent()
data class CloseConnectionIntent(val player: Player, val disconnectReason: DisconnectReason) : Intent()
