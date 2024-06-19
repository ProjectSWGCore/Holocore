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
package com.projectswg.holocore.services.gameplay.player.character

import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.player.Mail
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.gameplay.player.character.BankTipIntent
import com.projectswg.holocore.intents.gameplay.player.character.CashTipIntent
import com.projectswg.holocore.intents.support.global.chat.PersistentMessageIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class TippingService : Service() {
	
	@IntentHandler
	private fun handleCashTip(intent: CashTipIntent) {
		val amount = intent.amount
		val player = intent.sender
		val targetPlayer = intent.receiver
		val target = targetPlayer.creatureObject
		
		if (amount > player.creatureObject.cashBalance) {
			SystemMessageIntent.broadcastPersonal(player, ProsePackage(StringId("base_player", "prose_tip_nsf_cash"), "DI", amount, "TT", target.objectName))
			return
		}

		transferCash(player, amount, targetPlayer)
	}
	
	@IntentHandler
	private fun handleBankTip(intent: BankTipIntent) {
		val amount = intent.amount
		val player = intent.sender
		val targetPlayer = intent.receiver
		val target = targetPlayer.creatureObject
		
		if (amount > player.creatureObject.bankBalance) {
			SystemMessageIntent.broadcastPersonal(player, ProsePackage(StringId("base_player", "prose_tip_nsf_bank"), "DI", amount, "TT", target.objectName))
			return
		}

		val amountWithSurcharge = (amount * 1.05).toInt()	// System applies a 5% surcharge to the bank tip

		if (amountWithSurcharge > player.creatureObject.bankBalance) {
			SystemMessageIntent.broadcastPersonal(player, ProsePackage(StringId("base_player", "prose_tip_nsf_wire"), "DI", amountWithSurcharge, "TT", target.objectName))
			return
		}

		SuiMessageBox().run {
			title = "@base_player:tip_wire_title"
			prompt = "@base_player:tip_wire_prompt"
			addOkButtonCallback("tip") { _, _ -> wire(player, target, amount, amountWithSurcharge) }
			display(player)
		}
	}

	private fun transferCash(player: Player, amount: Int, targetPlayer: Player) {
		val target = targetPlayer.creatureObject
		player.creatureObject.setCashBalance((player.creatureObject.cashBalance - amount).toLong())
		target.setCashBalance((target.cashBalance + amount).toLong())
		StandardLog.onPlayerEvent(this, player, "tipped %d credits to %s using cash", amount, target)

		sendSystemMessages(player, amount, target, targetPlayer)
	}

	private fun sendSystemMessages(player: Player, amount: Int, target: CreatureObject, targetPlayer: Player) {
		SystemMessageIntent.broadcastPersonal(
			player, ProsePackage(StringId("base_player", "prose_tip_pass_self"), "DI", amount, "TT", target.objectName)
		)
		SystemMessageIntent.broadcastPersonal(
			targetPlayer, ProsePackage(StringId("base_player", "prose_tip_pass_target"), "TT", player.creatureObject.objectName, "DI", amount)
		)
	}

	private fun wire(player: Player, target: CreatureObject, amount: Int, amountWithSurcharge: Int) {
		player.creatureObject.setBankBalance((player.creatureObject.bankBalance - amountWithSurcharge).toLong())
		target.setBankBalance((target.bankBalance + amount).toLong())
		StandardLog.onPlayerEvent(this, player, "tipped %d credits to %s using wire transfer", amount, target)

		sendMails(target, amount, player)
	}

	private fun sendMails(target: CreatureObject, amount: Int, player: Player) {
		val targetMail = Mail("@money/acct_n:bank", "@base_player:wire_mail_subject", "", target.objectId)
		targetMail.outOfBandPackage = OutOfBandPackage(
			ProsePackage(StringId("base_player", "prose_wire_mail_target"), "DI", amount, "TO", player.creatureObject.objectName),
		)
		PersistentMessageIntent(target, targetMail, ProjectSWG.galaxy.name).broadcast()

		val selfMail = Mail(target.objectName, "@base_player:wire_mail_subject", "", player.creatureObject.objectId)
		selfMail.outOfBandPackage = OutOfBandPackage(
			ProsePackage(StringId("base_player", "prose_wire_mail_self"), "TO", target.objectName, "DI", amount),
		)
		PersistentMessageIntent(player.creatureObject, selfMail, ProjectSWG.galaxy.name).broadcast()
	}
}