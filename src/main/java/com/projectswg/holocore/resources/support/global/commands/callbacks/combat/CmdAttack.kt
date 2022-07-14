package com.projectswg.holocore.resources.support.global.commands.callbacks.combat

import com.projectswg.common.network.packets.swg.zone.ExecuteConsoleCommand
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdAttack : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creatureObject = player.creatureObject
		val equippedWeapon = creatureObject.equippedWeapon
		val weaponType = equippedWeapon.type
		val defaultAttack = weaponType.defaultAttack
		val executeConsoleCommand = ExecuteConsoleCommand()
		executeConsoleCommand.addCommand(defaultAttack)

		player.sendPacket(executeConsoleCommand)
	}
}