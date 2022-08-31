package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType

enum class ValidWeapon(private val num: Int, private val validWeaponChecker: ValidWeaponChecker) {
	FORCE_POWER(-5, ForcePowersValidWeaponChecker()),
	ALL_LIGHTSABERS(-4, AllLightsabersValidWeaponChecker()),
	ALL(-3, AllValidWeaponChecker()),
	MELEE(-2, MeleeValidWeaponChecker()),
	RANGED(-1, RangedValidWeaponChecker()),
	THROWN(8, ThrownValidWeaponChecker());

	fun isValid(weaponType: WeaponType): Boolean {
		return validWeaponChecker.isValid(weaponType)
	}

	companion object {
		fun getByNum(num: Int): ValidWeapon {
			return when (num) {
				FORCE_POWER.num -> FORCE_POWER
				ALL_LIGHTSABERS.num -> ALL_LIGHTSABERS
				ALL.num -> ALL
				MELEE.num -> MELEE
				RANGED.num -> RANGED
				THROWN.num -> THROWN
				else -> ALL
			}
		}
	}
}