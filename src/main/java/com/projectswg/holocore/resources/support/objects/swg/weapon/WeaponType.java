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
package com.projectswg.holocore.resources.support.objects.swg.weapon;

public enum WeaponType {
	RIFLE						(0),
	CARBINE						(1),
	PISTOL						(2),
	HEAVY						(3),
	ONE_HANDED_MELEE			(4),
	TWO_HANDED_MELEE			(5),
	UNARMED						(6),
	POLEARM_MELEE				(7),
	THROWN						(8),
	ONE_HANDED_SABER			(9),
	TWO_HANDED_SABER			(10),
	POLEARM_SABER				(11),
	HEAVY_WEAPON				(12),
	DIRECTIONAL_TARGET_WEAPON	(13),
	LIGHT_RIFLE					(14);
	
	private static final WeaponType [] VALUES = values();
	
	private int num;
	
	WeaponType(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static WeaponType getWeaponType(int num) {
		if (num < 0 || num >= VALUES.length)
			return RIFLE;
		return VALUES[num];
	}
	
	/**
	 * Determines whether this weapon type is in the melee category.
	 * Lightsabers are included in the melee category!
	 * @return {@code true} if this weapon type is in the melee category and {@code false} otherwise.
	 */
	public boolean isMelee() {
		switch (this) {
			case ONE_HANDED_MELEE:
			case TWO_HANDED_MELEE:
			case UNARMED:
			case POLEARM_MELEE: return true;
			default: return isLightsaber();	// If all else fails, resort to checking if this is a lightsaber
		}
	}
	
	/**
	 * Determines whether this weapon type is in the lightsaber category, which is a subcategory of the melee category.
	 * @return {@code true} if this weapon type is in the lightsaber category and {@code false} otherwise.
	 */
	public boolean isLightsaber() {
		switch (this) {
			case ONE_HANDED_SABER:
			case TWO_HANDED_SABER:
			case POLEARM_SABER: return true;
			default: return false;
		}
	}
}
