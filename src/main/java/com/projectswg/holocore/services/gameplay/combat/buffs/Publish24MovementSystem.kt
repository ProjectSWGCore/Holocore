/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.data.server_info.loader.MovementLoader


/**
 * In Publish 24 (final publish of the Combat Upgrade), the movement modifiers from buffs were changed for balance reasons.
 * Movement modifiers no longer stack, and only the strongest modifier is applied.
 * Negative effects (roots and snares) also take priority over positive effects (speed boosts).
 */
object Publish24MovementSystem {
	fun selectMovementModifier(modifiers: List<MovementLoader.MovementInfo>): MovementLoader.MovementInfo? {
		val roots = modifiers.filter { it.type == MovementLoader.MovementType.ROOT }
		if (roots.isNotEmpty())
			return roots.first()
		
		val snares = modifiers.filter { it.type == MovementLoader.MovementType.SNARE || it.type == MovementLoader.MovementType.PERMASNARE }
		if (snares.isNotEmpty())
			return snares.strongest()
		
		val boosts = modifiers.filter { it.type == MovementLoader.MovementType.BOOST || it.type == MovementLoader.MovementType.PERMABOOST }
		if (boosts.isNotEmpty())
			return boosts.strongest()
		
		return null
	}
}

private fun List<MovementLoader.MovementInfo>.strongest(): MovementLoader.MovementInfo {
	return this.maxBy { it.strength }
}
