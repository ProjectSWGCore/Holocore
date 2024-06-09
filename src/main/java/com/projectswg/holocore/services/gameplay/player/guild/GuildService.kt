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
package com.projectswg.holocore.services.gameplay.player.guild

import com.projectswg.common.data.objects.GameObjectType
import com.projectswg.common.network.packets.swg.zone.guild.GuildRequestMessage
import com.projectswg.common.network.packets.swg.zone.guild.GuildResponseMessage
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType
import com.projectswg.holocore.resources.support.objects.swg.guild.GuildObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

/**
 * Responsibilities:
 *
 *  1. Ensures existence of a singleton [GuildObject]
 *  2. Ensures every player is made aware of the [GuildObject]
 *  3. Responding to requests from players about which guild a specific player is in
 */
class GuildService : Service() {
	private var guildObject: GuildObject? = null
	
	override fun initialize(): Boolean {
		if (guildObject == null) {
			// A guild object doesn't already exist. Let's create one.
			val guildObject = ObjectCreator.createObjectFromTemplate("object/guild/shared_guild_object.iff") as GuildObject
			this.guildObject = guildObject
			ObjectCreatedIntent(guildObject).broadcast()
		}
		return super.start()
	}

	@IntentHandler
	private fun handleObjectCreated(intent: ObjectCreatedIntent) {
		val genericObject = intent.obj
		if (genericObject.gameObjectType == GameObjectType.GOT_GUILD) {
			guildObject = genericObject as GuildObject
		}
	}

	@IntentHandler
	private fun handlePlayerEvent(intent: PlayerEventIntent) {
		if (PlayerEvent.PE_ZONE_IN_SERVER == intent.event) {
			val creature = intent.player.creatureObject
			creature.setAware(AwarenessType.GUILD, setOf(guildObject))
		}
	}

	@IntentHandler
	private fun handleInboundPacketIntent(intent: InboundPacketIntent) {
		val packet = intent.packet
		if (packet is GuildRequestMessage) {
			val objectId = packet.objectId
			val response = GuildResponseMessage(objectId, "", "")
			val requester = intent.player
			requester.sendPacket(response)
		}
	}
}
