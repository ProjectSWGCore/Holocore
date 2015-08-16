var getOptions = function(options, player, target) {
	var use = new RadialOption(RadialItem.ITEM_USE);
	var reserve = new RadialOption(RadialItem.BANK_RESERVE);
	var creature = player.getCreatureObject();
	options.add(use);
	options.add(new RadialOption(RadialItem.EXAMINE));
	if (creature.getCurrentCity().equals("@corellia_region_names:coronet") ||
		creature.getCurrentCity().equals("@naboo_region_names:theed") || 
		creature.getCurrentCity().equals("@tatooine_region_names:mos_eisley"))
		options.add(reserve);
	// Bank Transfer/Safety Deposit
	use.addChild(RadialItem.BANK_TRANSFER);
	use.addChild(RadialItem.BANK_ITEMS);
	// Withdraw/Deposit
	if (creature.getBankBalance() > 0)
		use.addChild(RadialItem.BANK_WITHDRAW_ALL);
	if (creature.getCashBalance() > 0)
		use.addChild(RadialItem.BANK_DEPOSIT_ALL);
	// Galactic Reserve
	if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
		reserve.addChild(RadialItem.BANK_RESERVE_DEPOSIT);
	if (creature.getReserveBalance() > 0)
		reserve.addChild(RadialItem.BANK_RESERVE_WITHDRAW);
};
var handleSelection = function(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
		case RadialItem.BANK_TRANSFER: {
			creature = player.getCreatureObject();
			window = new SuiWindow("Script.transfer", player, SuiButtons.OK_CANCEL, '@base_player:bank_title', '@base_player:bank_prompt');
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
			window.display();
			break;
		}
		case RadialItem.BANK_ITEMS: {
			creature = player.getCreatureObject();
			var ClientOpenContainerMessage = Java.type("network.packets.swg.zone.ClientOpenContainerMessage");
			player.sendPacket(new ClientOpenContainerMessage(creature.getSlottedObject("bank").getObjectId(), ""));
			break;
		}
		case RadialItem.BANK_WITHDRAW_ALL: {
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
		case RadialItem.BANK_DEPOSIT_ALL: {
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
		case RadialItem.BANK_RESERVE_DEPOSIT: {
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
		case RadialItem.BANK_RESERVE_WITHDRAW: {
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
};
var handleBankTransfer = function(player, creature, eventType, parameters) {
	switch (eventType) {
		case SuiEvent.OK_PRESSED:
			creature.setCashBalance(Number(parameters.get('transaction.txtInputFrom.Text')));
			creature.setBankBalance(Number(parameters.get('transaction.txtInputTo.Text')));
			intentFactory.sendSystemMessage(player, '@base_player:bank_success');
			break;
	}
};