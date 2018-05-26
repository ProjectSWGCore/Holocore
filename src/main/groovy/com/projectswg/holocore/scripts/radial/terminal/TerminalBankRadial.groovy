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

package com.projectswg.holocore.scripts.radial.terminal

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow
import com.projectswg.holocore.scripts.radial.RadialHandlerInterface
import com.projectswg.holocore.utilities.IntentFactory

class TerminalBankRadial implements RadialHandlerInterface {
	
	def getOptions(List<RadialOption> options, Player player, SWGObject target) {
		def use = new RadialOption(RadialItem.ITEM_USE)
		def creature = player.getCreatureObject()
		
		options.add(use)
		options.add(new RadialOption(RadialItem.EXAMINE))
		
		// Bank Transfer/Safety Deposit
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU1, "@sui:bank_credits")
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU2, "@sui:bank_items")
		// Withdraw/Deposit
		if (creature.getBankBalance() > 0)
			use.addChildWithOverriddenText(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall")
		if (creature.getCashBalance() > 0)
			use.addChildWithOverriddenText(RadialItem.SERVER_MENU3, "@sui:bank_depositall")
		
		if (isInGalacticReserveCity(creature)) {
			def reserve = new RadialOption(RadialItem.SERVER_MENU50)
			reserve.setOverriddenText("@sui:bank_galactic_reserve")
			
			if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
				reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU49, "@sui:bank_galactic_reserve_deposit")
			if (creature.getReserveBalance() > 0)
				reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU48, "@sui:bank_galactic_reserve_withdraw")
			options.add(reserve)
		}
	}
	
	def handleSelection(Player player, SWGObject target, RadialItem selection) {
		def creature = player.getCreatureObject()
		switch (selection) {
			case RadialItem.ITEM_USE:
			case RadialItem.SERVER_MENU1:
				handleTransfer(player, creature)
				break
			case RadialItem.SERVER_MENU2:
				handleOpenBank(player, creature)
				break
			case RadialItem.SERVER_MENU3:
				handleBankDeposit(player, creature)
				break
			case RadialItem.SERVER_MENU4:
				handleBankWithdraw(player, creature)
				break
			case RadialItem.SERVER_MENU49:
				handleGalacticReserveDeposit(player, creature)
				break
			case RadialItem.SERVER_MENU48:
				handleGalacticReserveWithdraw(player, creature)
				break
		}
	}
	
	static def handleTransfer(Player player, CreatureObject creature) {
		def window = new SuiWindow("Script.transfer", SuiButtons.OK_CANCEL, '@base_player:bank_title', '@base_player:bank_prompt')
		window.setPropertyText('transaction.lblFrom', 'Cash')
		window.setPropertyText('transaction.lblTo', 'Bank')
		window.setPropertyText('transaction.lblStartingFrom', creature.getCashBalance().toString())
		window.setPropertyText('transaction.lblStartingTo', creature.getBankBalance().toString())
		window.setPropertyText('transaction.txtInputFrom', creature.getCashBalance().toString())
		window.setPropertyText('transaction.txtInputTo', creature.getBankBalance().toString())
		window.setProperty('transaction', 'ConversionRatioFrom', '1')
		window.setProperty('transaction', 'ConversionRatioTo', '1')
		window.addReturnableProperty('transaction.txtInputFrom', 'Text')
		window.addReturnableProperty('transaction.txtInputTo', 'Text')
		window.addCallback(SuiEvent.OK_PRESSED, "handleBankTransfer", {event,parameters -> handleBankTransfer(player, player.getCreatureObject(), parameters)})
		window.display(player)
	}
	
	static def handleOpenBank(Player player, CreatureObject creature) {
		player.sendPacket(new ClientOpenContainerMessage(creature.getSlottedObject("bank").getObjectId(), ""))
	}
	
	static def handleBankDeposit(Player player, CreatureObject creature) {
		def amount = creature.getCashBalance() as long
		creature.setBankBalance(amount + creature.getBankBalance())
		creature.setCashBalance(0l)
		if (amount > 0)
			IntentFactory.sendSystemMessage(player, "@base_player:prose_deposit_success", "DI", (int) amount)
		else
			IntentFactory.sendSystemMessage(player, '@error_message:bank_deposit')
	}
	
	static def handleBankWithdraw(Player player, CreatureObject creature) {
		def amount = creature.getBankBalance() as long
		creature.setCashBalance(creature.getCashBalance() + amount)
		creature.setBankBalance(0l)
		if (amount > 0)
			IntentFactory.sendSystemMessage(player, "@base_player:prose_withdraw_success", "DI", (int) amount)
		else
			IntentFactory.sendSystemMessage(player, '@error_message:bank_withdraw')
	}
	
	static def isInGalacticReserveCity(CreatureObject creature) {
		return creature.getCurrentCity() == "@corellia_region_names:coronet" || creature.getCurrentCity() == "@naboo_region_names:theed" || creature.getCurrentCity() == "@tatooine_region_names:mos_eisley"
	}
	
	static def handleGalacticReserveDeposit(Player player, CreatureObject creature) {
		if (!creature.canPerformGalacticReserveTransaction()) {
			IntentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction")
			return
		}
		def amount = creature.getBankBalance() as long
		if (amount > 1E9)
			amount = 1E9
		if (creature.getReserveBalance() + amount > 3E9 || amount == 0) {
			IntentFactory.sendSystemMessage(player, '@error_message:bank_deposit')
			return
		}
		creature.setBankBalance((creature.getBankBalance() - amount).longValue())
		creature.setReserveBalance((creature.getReserveBalance() + amount).longValue())
		creature.updateLastGalacticReserveTime()
	}
	
	static def handleGalacticReserveWithdraw(Player player, CreatureObject creature) {
		if (!creature.canPerformGalacticReserveTransaction()) {
			IntentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction")
			return
		}
		def amount = creature.getReserveBalance()
		if (amount > 1E9)
			amount = 1E9
		if (creature.getBankBalance() + amount > 2E9 || amount == 0) {
			IntentFactory.sendSystemMessage(player, '@error_message:bank_withdraw')
			return
		}
		creature.setBankBalance((creature.getBankBalance() + amount).longValue())
		creature.setReserveBalance((creature.getReserveBalance() - amount).longValue())
		creature.updateLastGalacticReserveTime()
	}
	
	static def handleBankTransfer(Player player, CreatureObject creature, Map<String, String> parameters) {
		creature.setCashBalance(Long.parseLong(parameters.get('transaction.txtInputFrom.Text')))
		creature.setBankBalance(Long.parseLong(parameters.get('transaction.txtInputTo.Text')))
		IntentFactory.sendSystemMessage(player, '@base_player:bank_success')
	}
}

