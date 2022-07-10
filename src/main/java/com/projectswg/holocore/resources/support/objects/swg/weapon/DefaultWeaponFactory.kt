package com.projectswg.holocore.resources.support.objects.swg.weapon

import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator

object DefaultWeaponFactory {
	fun createDefaultWeapon(): WeaponObject {
		val defWeapon = ObjectCreator.createObjectFromTemplate("object/weapon/melee/unarmed/shared_unarmed_default_player.iff") as WeaponObject?
		defWeapon ?: throw RuntimeException("Unable to create default weapon")
		ObjectCreatedIntent.broadcast(defWeapon)
		defWeapon.maxRange = 5f
		defWeapon.type = WeaponType.UNARMED
		defWeapon.attackSpeed = 4f
		defWeapon.minDamage = 10
		defWeapon.maxDamage = 20
		return defWeapon
	}
}