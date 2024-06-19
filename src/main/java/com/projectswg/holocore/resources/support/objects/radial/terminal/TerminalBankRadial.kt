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
package com.projectswg.holocore.resources.support.objects.radial.terminal

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class TerminalBankRadial : RadialHandlerInterface {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		val creature = player.creatureObject

		val useOptions: MutableList<RadialOption> = ArrayList()

		// Bank Transfer/Safety Deposit
		useOptions.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:bank_credits"))
		useOptions.add(RadialOption.create(RadialItem.SERVER_MENU2, "@sui:bank_items"))
		// Withdraw/Deposit
		if (creature.bankBalance > 0) useOptions.add(RadialOption.create(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall"))
		if (creature.cashBalance > 0) useOptions.add(RadialOption.create(RadialItem.SERVER_MENU3, "@sui:bank_depositall"))

		options.add(RadialOption.createSilent(RadialItem.ITEM_USE, useOptions))
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		val creature = player.creatureObject
		when (selection) {
			RadialItem.ITEM_USE, RadialItem.SERVER_MENU1 -> handleTransfer(player, creature)
			RadialItem.SERVER_MENU2                      -> handleOpenBank(player, creature)
			RadialItem.SERVER_MENU3                      -> handleBankDeposit(player, creature)
			RadialItem.SERVER_MENU4                      -> handleBankWithdraw(player, creature)
			else -> {}
		}
	}

	companion object {
		private fun handleTransfer(player: Player, creature: CreatureObject) {
			SuiWindow().run {
				suiScript = "Script.transfer"
				title = "@base_player:bank_title"
				prompt = "@base_player:bank_prompt"
				buttons = SuiButtons.OK_CANCEL
				setPropertyText("transaction.lblFrom", "Cash")
				setPropertyText("transaction.lblTo", "Bank")
				setPropertyText("transaction.lblStartingFrom", creature.cashBalance.toString())
				setPropertyText("transaction.lblStartingTo", creature.bankBalance.toString())
				setPropertyText("transaction.txtInputFrom", creature.cashBalance.toString())
				setPropertyText("transaction.txtInputTo", creature.bankBalance.toString())
				setProperty("transaction", "ConversionRatioFrom", "1")
				setProperty("transaction", "ConversionRatioTo", "1")
				addReturnableProperty("transaction.txtInputFrom", "Text")
				addReturnableProperty("transaction.txtInputTo", "Text")
				addCallback(SuiEvent.OK_PRESSED, "handleBankTransfer") { _: SuiEvent, parameters: Map<String, String> -> handleBankTransfer(player, player.creatureObject, parameters) }
				display(player)
			}
		}

		private fun handleOpenBank(player: Player, creature: CreatureObject) {
			player.sendPacket(ClientOpenContainerMessage(creature.getSlottedObject("bank").objectId, ""))
		}

		private fun handleBankDeposit(player: Player, creature: CreatureObject) {
			val amount = creature.cashBalance.toLong()
			creature.setBankBalance(amount + creature.bankBalance)
			creature.setCashBalance(0L)
			if (amount > 0) broadcastPersonal(player, ProsePackage(StringId("@base_player:prose_deposit_success"), "DI", amount.toInt()))
			else broadcastPersonal(player, "@error_message:bank_deposit")
		}

		private fun handleBankWithdraw(player: Player, creature: CreatureObject) {
			val amount = creature.bankBalance.toLong()
			creature.setCashBalance(creature.cashBalance + amount)
			creature.setBankBalance(0L)
			if (amount > 0) broadcastPersonal(player, ProsePackage(StringId("@base_player:prose_withdraw_success"), "DI", amount.toInt()))
			else broadcastPersonal(player, "@error_message:bank_withdraw")
		}

		private fun handleBankTransfer(player: Player, creature: CreatureObject, parameters: Map<String, String>) {
			creature.setCashBalance(parameters["transaction.txtInputFrom.Text"]!!.toLong())
			creature.setBankBalance(parameters["transaction.txtInputTo.Text"]!!.toLong())
			broadcastPersonal(player, "@base_player:bank_success")
		}
	}
}
