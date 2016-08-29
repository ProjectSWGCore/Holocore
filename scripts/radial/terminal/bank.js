function getOptions(options, player, target) {
	var use = new RadialOption(RadialItem.ITEM_USE);
	var reserve = new RadialOption(RadialItem.SERVER_MENU50);
	var creature = player.getCreatureObject();
    
    reserve.setOverriddenText("@sui:bank_galactic_reserve");
    
	options.add(use);
	options.add(new RadialOption(RadialItem.EXAMINE));
	if (creature.getCurrentCity().equals("@corellia_region_names:coronet") ||
		creature.getCurrentCity().equals("@naboo_region_names:theed") || 
		creature.getCurrentCity().equals("@tatooine_region_names:mos_eisley"))
		options.add(reserve);
	// Bank Transfer/Safety Deposit
	use.addChildWithOverriddenText(RadialItem.SERVER_MENU1, "@sui:bank_credits");
	use.addChildWithOverriddenText(RadialItem.SERVER_MENU2, "@sui:bank_items");
	// Withdraw/Deposit
	if (creature.getBankBalance() > 0)
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall");
	if (creature.getCashBalance() > 0)
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU3, "@sui:bank_depositall");
	// Galactic Reserve
	if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
		reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU49, "@sui:bank_galactic_reserve_deposit");
	if (creature.getReserveBalance() > 0)
		reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU48, "@sui:bank_galactic_reserve_withdraw");
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
		case RadialItem.SERVER_MENU1: {
			creature = player.getCreatureObject();
			window = new SuiWindow("Script.transfer", SuiButtons.OK_CANCEL, '@base_player:bank_title', '@base_player:bank_prompt');
			window.setPropertyText('transaction.lblFrom', 'Cash');
			window.setPropertyText('transaction.lblTo', 'Bank');
			window.setPropertyText('transaction.lblStartingFrom', creature.getCashBalance());
			window.setPropertyText('transaction.lblStartingTo', creature.getBankBalance());
			window.setPropertyText('transaction.txtInputFrom', creature.getCashBalance());
			window.setPropertyText('transaction.txtInputTo', creature.getBankBalance());
			window.setProperty('transaction', 'ConversionRatioFrom', '1');
			window.setProperty('transaction', 'ConversionRatioTo', '1');
			window.addReturnableProperty('transaction.txtInputFrom', 'Text');
			window.addReturnableProperty('transaction.txtInputTo', 'Text');
			window.addCallback("radial/terminal/bank", "handleBankTransfer");
			window.display(player);
			break;
		}
		case RadialItem.SERVER_MENU2: {
			creature = player.getCreatureObject();
			var ClientOpenContainerMessage = Java.type("network.packets.swg.zone.ClientOpenContainerMessage");
			player.sendPacket(new ClientOpenContainerMessage(creature.getSlottedObject("bank").getObjectId(), ""));
			break;
		}
		case RadialItem.SERVER_MENU4: {
			creature = player.getCreatureObject();
			amount = creature.getBankBalance();
			creature.setCashBalance(creature.getCashBalance() + amount);
			creature.setBankBalance(0);
			if (amount > 0)
				intentFactory.sendSystemMessage(player, "@base_player:prose_withdraw_success", "DI", new java.lang.Integer(amount));
			else
				intentFactory.sendSystemMessage(player, '@error_message:bank_withdraw');
			break;
		}
		case RadialItem.SERVER_MENU3: {
			creature = player.getCreatureObject();
			amount = creature.getCashBalance();
			creature.setBankBalance(amount + creature.getBankBalance());
			creature.setCashBalance(0);
			if (amount > 0)
				intentFactory.sendSystemMessage(player, "@base_player:prose_deposit_success", "DI", new java.lang.Integer(amount));
			else
				intentFactory.sendSystemMessage(player, '@error_message:bank_deposit');
			break;
		}
		case RadialItem.SERVER_MENU49: {
			creature = player.getCreatureObject();
			if (!creature.canPerformGalacticReserveTransaction()) {
				intentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction");
				break;
			}
			amount = creature.getBankBalance();
			if (amount > 1E9)
				amount = 1E9;
			if (creature.getReserveBalance() + amount > 3E9 || amount == 0) {
				intentFactory.sendSystemMessage(player, '@error_message:bank_deposit');
				break;
			}
			creature.setBankBalance(creature.getBankBalance() - amount);
			creature.setReserveBalance(creature.getReserveBalance() + amount);
			creature.updateLastGalacticReserveTime();
			break;
		}
		case RadialItem.SERVER_MENU48: {
			creature = player.getCreatureObject();
			if (!creature.canPerformGalacticReserveTransaction()) {
				intentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction");
				break;
			}
			amount = creature.getReserveBalance();
			if (amount > 1E9)
				amount = 1E9;
			if (creature.getBankBalance() + amount > 2E9 || amount == 0) {
				intentFactory.sendSystemMessage(player, '@error_message:bank_withdraw');
				break;
			}
			creature.setBankBalance(creature.getBankBalance() + amount);
			creature.setReserveBalance(creature.getReserveBalance() - amount);
			creature.updateLastGalacticReserveTime();
			break;
		}
	}
}

function handleBankTransfer(player, creature, eventType, parameters) {
	switch (eventType) {
		case SuiEvent.OK_PRESSED:
			creature.setCashBalance(Number(parameters.get('transaction.txtInputFrom.Text')));
			creature.setBankBalance(Number(parameters.get('transaction.txtInputTo.Text')));
			intentFactory.sendSystemMessage(player, '@base_player:bank_success');
			break;
	}
}