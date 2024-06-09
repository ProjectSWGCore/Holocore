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
package com.projectswg.holocore.resources.support.objects.radial.terminal;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TerminalBankRadial implements RadialHandlerInterface {
	
	public TerminalBankRadial() {
		
	}
	
	@Override
	public void getOptions(@NotNull Collection<RadialOption> options, @NotNull Player player, @NotNull SWGObject target) {
		CreatureObject creature = player.getCreatureObject();

		List<RadialOption> useOptions = new ArrayList<>();

		// Bank Transfer/Safety Deposit
		useOptions.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:bank_credits"));
		useOptions.add(RadialOption.create(RadialItem.SERVER_MENU2, "@sui:bank_items"));
		// Withdraw/Deposit
		if (creature.getBankBalance() > 0)
			useOptions.add(RadialOption.create(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall"));
		if (creature.getCashBalance() > 0)
			useOptions.add(RadialOption.create(RadialItem.SERVER_MENU3, "@sui:bank_depositall"));

		options.add(RadialOption.createSilent(RadialItem.ITEM_USE, useOptions));
	}
	
	@Override
	public void handleSelection(@NotNull Player player, @NotNull SWGObject target, @NotNull RadialItem selection) {
		CreatureObject creature = player.getCreatureObject();
		switch (selection) {
			case ITEM_USE:
			case SERVER_MENU1:
				handleTransfer(player, creature);
				break;
			case SERVER_MENU2:
				handleOpenBank(player, creature);
				break;
			case SERVER_MENU3:
				handleBankDeposit(player, creature);
				break;
			case SERVER_MENU4:
				handleBankWithdraw(player, creature);
				break;
		}
	}
	
	private static void handleTransfer(Player player, CreatureObject creature) {
		SuiWindow window = new SuiWindow("Script.transfer", SuiButtons.OK_CANCEL, "@base_player:bank_title", "@base_player:bank_prompt");
		window.setPropertyText("transaction.lblFrom", "Cash");
		window.setPropertyText("transaction.lblTo", "Bank");
		window.setPropertyText("transaction.lblStartingFrom", Long.toString(creature.getCashBalance()));
		window.setPropertyText("transaction.lblStartingTo", Long.toString(creature.getBankBalance()));
		window.setPropertyText("transaction.txtInputFrom", Long.toString(creature.getCashBalance()));
		window.setPropertyText("transaction.txtInputTo", Long.toString(creature.getBankBalance()));
		window.setProperty("transaction", "ConversionRatioFrom", "1");
		window.setProperty("transaction", "ConversionRatioTo", "1");
		window.addReturnableProperty("transaction.txtInputFrom", "Text");
		window.addReturnableProperty("transaction.txtInputTo", "Text");
		window.addCallback(SuiEvent.OK_PRESSED, "handleBankTransfer", (event, parameters) -> handleBankTransfer(player, player.getCreatureObject(), parameters));
		window.display(player);
	}
	
	private static void handleOpenBank(Player player, CreatureObject creature) {
		player.sendPacket(new ClientOpenContainerMessage(creature.getSlottedObject("bank").getObjectId(), ""));
	}
	
	private static void handleBankDeposit(Player player, CreatureObject creature) {
		long amount = creature.getCashBalance();
		creature.setBankBalance(amount + creature.getBankBalance());
		creature.setCashBalance(0L);
		if (amount > 0)
			SystemMessageIntent.broadcastPersonal(player, new ProsePackage(new StringId("@base_player:prose_deposit_success"), "DI", (int) amount));
		else
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_deposit");
	}
	
	private static void handleBankWithdraw(Player player, CreatureObject creature) {
		long amount = creature.getBankBalance();
		creature.setCashBalance(creature.getCashBalance() + amount);
		creature.setBankBalance(0L);
		if (amount > 0)
			SystemMessageIntent.broadcastPersonal(player, new ProsePackage(new StringId("@base_player:prose_withdraw_success"), "DI", (int) amount));
		else
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_withdraw");
	}
	
	private static void handleBankTransfer(Player player, CreatureObject creature, Map<String, String> parameters) {
		creature.setCashBalance(Long.parseLong(parameters.get("transaction.txtInputFrom.Text")));
		creature.setBankBalance(Long.parseLong(parameters.get("transaction.txtInputTo.Text")));
		SystemMessageIntent.broadcastPersonal(player, "@base_player:bank_success");
	}
	
}
