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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.gameplay.player.character.BankTipIntent
import com.projectswg.holocore.intents.gameplay.player.character.CashTipIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

private enum class TipType {
	CASH,
	BANK
}

class CmdTip : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val amount = args.replace(" bank", "").toIntOrNull()
		
		if (amount == null) {
			SystemMessageIntent.broadcastPersonal(player, ProsePackage(StringId("base_player", "tip_syntax")))
			return
		}
		
		if (amount <= 0) {
			SystemMessageIntent.broadcastPersonal(player, ProsePackage(StringId("base_player", "prose_tip_invalid_amt"), "DI", amount))
			return
		}
		
		if (target !is CreatureObject) {
			SystemMessageIntent.broadcastPersonal(player, "You cannot tip a non-player object")
			return
		}

		val targetPlayer = target.owner
		
		if (targetPlayer == null) {
			SystemMessageIntent.broadcastPersonal(player, "You cannot tip a non-player object")
			return
		}
		
		if (target == player.creatureObject) {
			SystemMessageIntent.broadcastPersonal(player, "You cannot tip yourself")
			return
		}

		val tipType = tipType(args, player, target)
		if (tipType == TipType.CASH) {
			CashTipIntent(player, targetPlayer, amount).broadcast()
		} else if (tipType == TipType.BANK) {
			BankTipIntent(player, targetPlayer, amount).broadcast()
		}
	}

	private fun tipType(args: String, player: Player, target: SWGObject): TipType {
		if (args.endsWith(" bank")) {
			return TipType.BANK
		}

		if (player.creatureObject.terrain != target.terrain) {
			return TipType.BANK
		}

		if (player.creatureObject.worldLocation.distanceTo(target.worldLocation) > 16) {
			return TipType.BANK
		}

		return TipType.CASH
	}
}
