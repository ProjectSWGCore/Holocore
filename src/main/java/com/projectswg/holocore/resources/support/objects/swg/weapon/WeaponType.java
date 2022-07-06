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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public enum WeaponType {
	RIFLE						(0, "rifle_speed", WeaponClass.RANGED, "rifle_defense", "rifle_accuracy"),
	CARBINE						(1, "carbine_speed", WeaponClass.RANGED, "carbine_defense", "carbine_accuracy"),
	PISTOL						(2, "pistol_speed", WeaponClass.RANGED, "pistol_defense", "pistol_accuracy"),
	HEAVY						(3, "heavyweapon_speed", WeaponClass.RANGED, "heavyweapon_defense", "heavyweapon_accuracy"),
	ONE_HANDED_MELEE			(4, "onehandmelee_speed", WeaponClass.MELEE, "onehandmelee_defense", "onehandmelee_accuracy"),
	TWO_HANDED_MELEE			(5, "twohandmelee_speed", WeaponClass.MELEE, "twohandmelee_defense", "twohandmelee_accuracy"),
	UNARMED						(6, "unarmed_speed", WeaponClass.MELEE, "unarmed_defense", "unarmed_accuracy"),
	POLEARM_MELEE				(7, "polearm_speed", WeaponClass.MELEE, "polearm_defense", "polearm_accuracy"),
	THROWN						(8, "thrown_speed", WeaponClass.RANGED, null, "thrown_accuracy"),
	ONE_HANDED_SABER			(9, "onehandlightsaber_speed", WeaponClass.MELEE, null, "onehandlightsaber_accuracy"),
	TWO_HANDED_SABER			(10, "twohandlightsaber_speed", WeaponClass.MELEE, null, "twohandlightsaber_accuracy"),
	POLEARM_SABER				(11, "polearmlightsaber_speed", WeaponClass.MELEE, null, "polearmlightsaber_accuracy"),
	
	// TODO these are NGE weapon types we should remove later
	HEAVY_WEAPON				(12, "unavailable", WeaponClass.RANGED, null, null),
	DIRECTIONAL_TARGET_WEAPON	(13, "unavailable", WeaponClass.RANGED, null, null),
	LIGHT_RIFLE					(14, "unavailable", WeaponClass.RANGED, null, null);
	
	private static final WeaponType [] VALUES = values();
	
	private final int num;
	private final String speedSkillMod;
	private final WeaponClass weaponClass;
	private final String defenseSkillMod;
	private final String accuracySkillMod;
	
	WeaponType(int num, String speedSkillMod, WeaponClass weaponClass, String defenseSkillMod, String accuracySkillMod) {
		this.num = num;
		this.speedSkillMod = speedSkillMod;
		this.weaponClass = weaponClass;
		this.defenseSkillMod = defenseSkillMod;
		this.accuracySkillMod = accuracySkillMod;
	}
	
	public int getNum() {
		return num;
	}
	
	/**
	 *
	 * @return e.g. Unarmed Speed and Melee Speed
	 */
	public Collection<String> getSpeedSkillMods() {
		return Arrays.asList(speedSkillMod, weaponClass.getSpeedSkillMod());
	}
	
	/**
	 *
	 * @return e.g. Unarmed Defense
	 */
	public String getDefenseSkillMod() {
		return defenseSkillMod;
	}
	
	public WeaponClass getWeaponClass() {
		return weaponClass;
	}
	
	public Collection<String> getAccuracySkillMods() {
		Collection<String> accuracySkillMods = new HashSet<>();
		
		if (accuracySkillMod != null) {
			accuracySkillMods.add(accuracySkillMod);
		}
		
		accuracySkillMods.add(weaponClass.getAccuracySkillMod());
		
		return accuracySkillMods;
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
		return weaponClass == WeaponClass.MELEE;
	}
	
	/**
	 * Determines whether this weapon type is in the lightsaber category, which is a subcategory of the melee category.
	 * @return {@code true} if this weapon type is in the lightsaber category and {@code false} otherwise.
	 */
	public boolean isLightsaber() {
		return switch (this) {
			case ONE_HANDED_SABER, TWO_HANDED_SABER, POLEARM_SABER -> true;
			default -> false;
		};
	}
	
	/**
	 * Determines whether this weapon type is in the ranged category.
	 * @return {@code true} if this weapon type is in the ranged category and {@code false} otherwise.
	 */
	public boolean isRanged() {
		return weaponClass == WeaponClass.RANGED;
	}
}
