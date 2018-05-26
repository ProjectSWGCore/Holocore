/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects;

import java.util.Arrays;

public enum GameObjectTypeMask {
	GOTM_NONE					(0x00000000),
	GOTM_ARMOR					(0x00000100),
	GOTM_BUILDING				(0x00000200),
	GOTM_CREATURE				(0x00000400),
	GOTM_DATA					(0x00000800),
	GOTM_INSTALLATION			(0x00001000),
	GOTM_CHRONICLES				(0x00001100),
	GOTM_MISC					(0x00002000),
	GOTM_TERMINAL				(0x00004000),
	GOTM_TOOL					(0x00008000),
	GOTM_VEHICLE				(0x00010000),
	GOTM_WEAPON					(0x00020000),
	GOTM_COMPONENT				(0x00040000),
	GOTM_POWERUP_WEAPON			(0x00100000),
	GOTM_JEWELRY				(0x00200000),
	GOTM_RESOURCE_CONTAINER		(0x00400000),
	GOTM_DEED					(0x00800000),
	GOTM_CLOTHING				(0x01000000),
	GOTM_SHIP					(0x20000000),
	GOTM_CYBERNETIC				(0x20000100),
	GOTM_SHIP_COMPONENT			(0x40000000);
	
	private static final GameObjectTypeMask [] MASKS;
	private static final long [] MASK_LIST;
	
	static {
		MASKS = values();
		MASK_LIST = new long[MASKS.length];
		for (int i = 0; i < MASKS.length; i++) {
			MASK_LIST[i] = MASKS[i].getMask() & 0xFFFFFFFFL;
		}
	}
	
	private int mask;
	
	GameObjectTypeMask(int mask) {
		this.mask = mask;
	}
	
	public int getMask() {
		return mask;
	}
	
	public static GameObjectTypeMask getFromMask(long mask) {
		int ind = Arrays.binarySearch(MASK_LIST, mask & 0xFFFFFFFFL);
		if (ind < 0)
			return GOTM_NONE;
		return MASKS[ind];
	}
}
