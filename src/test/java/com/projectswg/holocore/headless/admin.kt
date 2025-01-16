/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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

import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.zone.UpdateContainmentMessage
import com.projectswg.common.network.packets.swg.zone.UpdateTransformMessage
import com.projectswg.common.network.packets.swg.zone.UpdateTransformWithParentMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.PostureUpdate
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.concurrent.TimeUnit

/**
 * Admin command /kill
 * @param target The object to kill
 */
fun ZonedInCharacter.adminKill(target: SWGObject?) {
	sendCommand("kill", target)
	player.waitForNextPacket(PostureUpdate::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("No known packet received")
}

/**
 * Admin command /grantSkill
 * @param skill the skill to grant
 */
fun ZonedInCharacter.adminGrantSkill(skill: String) {
	sendCommand("grantSkill", args = skill)
	player.waitForNextObjectDelta(player.creatureObject.objectId, 4, 14, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("No known packet received")
}

/**
 * Admin command /teleport
 * @param target The object to teleport. If null, the player executing the command will be teleported.
 * @param planet The planet to teleport to
 * @param x The x coordinate to teleport to
 * @param y The y coordinate to teleport to
 * @param z The z coordinate to teleport to
 */
fun ZonedInCharacter.adminTeleport(target: SWGObject? = null, planet: Terrain, x: Number, y: Number, z: Number) {
	val argList = mutableListOf<String>()
	if (target != null) {
		val targetFirstName = target.objectName.split(" ").first()
		argList.add(targetFirstName)
	}

	argList.add(planet.getName())
	argList.add(x.toInt().toString())
	argList.add(y.toInt().toString())
	argList.add(z.toInt().toString())

	sendCommand("teleport", args = argList.joinToString(separator = " "))

	player.waitForNextPacket(setOf(UpdateContainmentMessage::class.java, UpdateTransformMessage::class.java, UpdateTransformWithParentMessage::class.java), 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("No known packet received")
}
