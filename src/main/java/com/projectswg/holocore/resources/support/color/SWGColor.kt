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
package com.projectswg.holocore.resources.support.color

import com.projectswg.common.data.RGB

object SWGColor {
	val black = RGB(0, 0, 0)

	object Blues {
		val cyan = RGB(0, 255, 255)
	}

	object Greens {
		val seagreen = RGB(46, 139, 87)
		val lawngreen = RGB(124, 252, 0)
	}
	
	object Grays {
		val gray = RGB(190, 190, 190)
		val dimgray = RGB(105, 105, 105)
		val lightgray = RGB(211, 211, 211)
		val lightslategray = RGB(119, 136, 153)
		val slategray = RGB(112, 128, 144)
	}
	
	object Oranges {
		val orange = RGB(255, 165, 0)
	}

	object Reds {
		val orangered = RGB(255, 69, 0)
		val red = RGB(255, 0, 0)
	}

	object Violets {
		val magenta = RGB(255, 0, 255)
	}

	object Whites {
		val white = RGB(255, 255, 255)
	}
}