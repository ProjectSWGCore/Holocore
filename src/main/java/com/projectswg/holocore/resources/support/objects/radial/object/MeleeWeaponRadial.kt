package com.projectswg.holocore.resources.support.objects.radial.`object`

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.gameplay.jedi.OpenLightsaberIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject

class MeleeWeaponRadial : RadialHandlerInterface {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		if (target is WeaponObject) {
			if (target.lightsaberInventory != null) {
				options.add(RadialOption.create(RadialItem.SERVER_MENU1, "@jedi_spam:open_saber"))
			}
		}
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		if (selection == RadialItem.SERVER_MENU1) {
			OpenLightsaberIntent.broadcast(player, target as WeaponObject)
		}
	}
}