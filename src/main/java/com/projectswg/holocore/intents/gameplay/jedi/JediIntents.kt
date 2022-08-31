package com.projectswg.holocore.intents.gameplay.jedi

import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import me.joshlarson.jlcommon.control.Intent

data class CreateTestLightsaberIntent(val player: Player): Intent() {
	companion object {
		@JvmStatic
		fun broadcast(player: Player) = CreateTestLightsaberIntent(player).broadcast()
	}
}

data class OpenLightsaberIntent(val player: Player, val weaponObject: WeaponObject): Intent() {
	companion object {
		@JvmStatic
		fun broadcast(player: Player, weaponObject: WeaponObject) = OpenLightsaberIntent(player, weaponObject).broadcast()
	}
}

data class TuneCrystalNowIntent(val tuner: CreatureObject, val crystal: TangibleObject): Intent() {
	companion object {
		@JvmStatic
		fun broadcast(tuner: CreatureObject, crystal: TangibleObject) = TuneCrystalNowIntent(tuner, crystal).broadcast()
	}
}

data class RequestTuneCrystalIntent(val tuner: CreatureObject, val crystal: TangibleObject): Intent() {
	companion object {
		@JvmStatic
		fun broadcast(tuner: CreatureObject, crystal: TangibleObject) = RequestTuneCrystalIntent(tuner, crystal).broadcast()
	}
}