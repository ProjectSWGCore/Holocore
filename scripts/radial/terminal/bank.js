var getOptions = function(options, player, target) {
	use = new RadialOption(RadialItem.ITEM_USE);
	use.addChild(new RadialOption(RadialItem.BANK_TRANSFER));
	use.addChild(new RadialOption(RadialItem.BANK_ITEMS));
	options.add(use);
	options.add(new RadialOption(RadialItem.EXAMINE));
	creature = player.getCreatureObject();
	if (creature.getBankBalance() > 0)
		use.addChild(new RadialOption(RadialItem.BANK_WITHDRAW_ALL));
	if (creature.getCashBalance() > 0)
		use.addChild(new RadialOption(RadialItem.BANK_DEPOSIT_ALL));
	if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9 || creature.getReserveBalance() > 0) {
		reserve = new RadialOption(RadialItem.BANK_RESERVE);
		if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
			reserve.addChild(new RadialOption(RadialItem.BANK_RESERVE_DEPOSIT));
		if (creature.getReserveBalance() > 0)
			reserve.addChild(new RadialOption(RadialItem.BANK_RESERVE_WITHDRAW));
		options.add(reserve);
	}
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
				intentFactory.sendSystemMessage(player, "You successfully withdraw " + amount + " credits from your account.");
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
				intentFactory.sendSystemMessage(player, "You successfully deposit " + amount + " credits to your account.");
			else
				intentFactory.sendSystemMessage(player, '@error_message:bank_deposit');
			break;
		}
		case RadialItem.BANK_RESERVE_DEPOSIT: {
			creature = player.getCreatureObject();
			amount = creature.getBankBalance();
			if (amount > 1E9)
				amount = 1E9;
			if (creature.getReserveBalance() + amount > 3E9 || amount == 0) {
				intentFactory.sendSystemMessage(player, '@error_message:bank_deposit');
				break;
			}
			creature.setBankBalance(creature.getBankBalance() - amount);
			creature.setReserveBalance(creature.getReserveBalance() + amount);
			break;
		}
		case RadialItem.BANK_RESERVE_WITHDRAW: {
			creature = player.getCreatureObject();
			amount = creature.getReserveBalance();
			if (amount > 1E9)
				amount = 1E9;
			if (creature.getBankBalance() + amount > 2E9 || amount == 0) {
				intentFactory.sendSystemMessage(player, '@error_message:bank_withdraw');
				break;
			}
			creature.setBankBalance(creature.getBankBalance() + amount);
			creature.setReserveBalance(creature.getReserveBalance() - amount);
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