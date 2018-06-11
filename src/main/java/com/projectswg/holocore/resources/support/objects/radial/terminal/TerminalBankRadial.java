package com.projectswg.holocore.resources.support.objects.radial.terminal;

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
import com.projectswg.holocore.utilities.IntentFactory;

import java.util.List;
import java.util.Map;

public class TerminalBankRadial implements RadialHandlerInterface {
	
	public TerminalBankRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		RadialOption use = new RadialOption(RadialItem.ITEM_USE);
		CreatureObject creature = player.getCreatureObject();
		
		options.add(use);
		options.add(new RadialOption(RadialItem.EXAMINE));
		
		// Bank Transfer/Safety Deposit
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU1, "@sui:bank_credits");
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU2, "@sui:bank_items");
		// Withdraw/Deposit
		if (creature.getBankBalance() > 0)
			use.addChildWithOverriddenText(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall");
		if (creature.getCashBalance() > 0)
			use.addChildWithOverriddenText(RadialItem.SERVER_MENU3, "@sui:bank_depositall");
		
		if (isInGalacticReserveCity(creature)) {
			RadialOption reserve = new RadialOption(RadialItem.SERVER_MENU50);
			reserve.setOverriddenText("@sui:bank_galactic_reserve");
			
			if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
				reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU49, "@sui:bank_galactic_reserve_deposit");
			if (creature.getReserveBalance() > 0)
				reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU48, "@sui:bank_galactic_reserve_withdraw");
			options.add(reserve);
		}
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
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
			case SERVER_MENU49:
				handleGalacticReserveDeposit(player, creature);
				break;
			case SERVER_MENU48:
				handleGalacticReserveWithdraw(player, creature);
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
			IntentFactory.sendSystemMessage(player, "@base_player:prose_deposit_success", "DI", (int) amount);
		else
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_deposit");
	}
	
	private static void handleBankWithdraw(Player player, CreatureObject creature) {
		long amount = creature.getBankBalance();
		creature.setCashBalance(creature.getCashBalance() + amount);
		creature.setBankBalance(0L);
		if (amount > 0)
			IntentFactory.sendSystemMessage(player, "@base_player:prose_withdraw_success", "DI", (int) amount);
		else
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_withdraw");
	}
	
	private static boolean isInGalacticReserveCity(CreatureObject creature) {
		return creature.getCurrentCity().equals("@corellia_region_names:coronet")
				|| creature.getCurrentCity().equals("@naboo_region_names:theed")
				|| creature.getCurrentCity().equals("@tatooine_region_names:mos_eisley");
	}
	
	private static void handleGalacticReserveDeposit(Player player, CreatureObject creature) {
		if (!creature.canPerformGalacticReserveTransaction()) {
			SystemMessageIntent.broadcastPersonal(player, "You have to wait to perform another Galactic Reserve transaction");
			return;
		}
		long amount = creature.getBankBalance();
		if (amount > 1E9)
			amount = (long) 1E9;
		if (creature.getReserveBalance() + amount > 3E9 || amount == 0) {
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_deposit");
			return;
		}
		creature.setBankBalance((creature.getBankBalance() - amount));
		creature.setReserveBalance((creature.getReserveBalance() + amount));
		creature.updateLastGalacticReserveTime();
	}
	
	private static void handleGalacticReserveWithdraw(Player player, CreatureObject creature) {
		if (!creature.canPerformGalacticReserveTransaction()) {
			SystemMessageIntent.broadcastPersonal(player, "You have to wait to perform another Galactic Reserve transaction");
			return;
		}
		long amount = creature.getReserveBalance();
		if (amount > 1E9)
			amount = (long) 1E9;
		if (creature.getBankBalance() + amount > 2E9 || amount == 0) {
			SystemMessageIntent.broadcastPersonal(player, "@error_message:bank_withdraw");
			return;
		}
		creature.setBankBalance(creature.getBankBalance() + amount);
		creature.setReserveBalance(creature.getReserveBalance() - amount);
		creature.updateLastGalacticReserveTime();
	}
	
	private static void handleBankTransfer(Player player, CreatureObject creature, Map<String, String> parameters) {
		creature.setCashBalance(Long.parseLong(parameters.get("transaction.txtInputFrom.Text")));
		creature.setBankBalance(Long.parseLong(parameters.get("transaction.txtInputTo.Text")));
		SystemMessageIntent.broadcastPersonal(player, "@base_player:bank_success");
	}
	
}
