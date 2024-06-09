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
package com.projectswg.holocore.intents.support.global.zone

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.Intent

class RequestZoneInIntent(val creature: CreatureObject) : Intent()

class NotifyPlayersPacketIntent(val packet: SWGPacket, val terrain: Terrain?, val condition: ConditionalNotify?, val networkIds: List<Long>?) : Intent() {
	constructor(packet: SWGPacket, condition: ConditionalNotify?, networkIds: List<Long>?) : this(packet, null, condition, networkIds)

	constructor(packet: SWGPacket, terrain: Terrain?) : this(packet, terrain, null, null)

	constructor(packet: SWGPacket, networkIds: List<Long>?) : this(packet, null, null, networkIds)

	constructor(p: SWGPacket) : this(p, null, null, null)

	interface ConditionalNotify {
		fun meetsCondition(player: Player): Boolean
	}
}

class PlayerEventIntent(val player: Player, val event: PlayerEvent) : Intent()
class PlayerTransformedIntent(val player: CreatureObject, val oldParent: SWGObject?, val newParent: SWGObject?, val oldLocation: Location, val newLocation: Location) : Intent()