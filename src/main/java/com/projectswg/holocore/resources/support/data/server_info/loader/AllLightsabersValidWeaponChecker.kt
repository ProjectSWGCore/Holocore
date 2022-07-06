package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType

class AllLightsabersValidWeaponChecker : ValidWeaponChecker {
	override fun isValid(weaponType: WeaponType): Boolean {
		return weaponType.isLightsaber
	}
}