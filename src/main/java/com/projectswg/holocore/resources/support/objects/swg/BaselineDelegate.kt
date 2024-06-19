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

package com.projectswg.holocore.resources.support.objects.swg

import com.projectswg.common.encoding.StringType
import kotlin.reflect.KProperty

class BaselineDelegate<T>(private var value: T, private val page: Int, private val update: Int, private val stringType: StringType? = null) {
	
	operator fun getValue(thisRef: SWGObject, property: KProperty<*>): T {
		return value
	}
	
	operator fun setValue(thisRef: SWGObject, property: KProperty<*>, value: T) {
		this.value = value
		if (stringType != null) {
			thisRef.sendDelta(page, update, value, stringType)
		} else {
			thisRef.sendDelta(page, update, value)
		}
	}
	
}

class TransformedBaselineDelegate<T, R>(private var value: T, private val page: Int, private val update: Int, private val transformer: (T) -> R) {

	operator fun getValue(thisRef: SWGObject, property: KProperty<*>): T {
		return value
	}

	operator fun setValue(thisRef: SWGObject, property: KProperty<*>, value: T) {
		this.value = value
		thisRef.sendDelta(page, update, transformer(value))
	}

}
