var getOptions = function(options, player, target) {
	// Standard
	use = new RadialOption(RadialItem.ITEM_USE);
	options.add(use);
	options.add(new RadialOption(RadialItem.EXAMINE));
	creature = player.getCreatureObject();
	// Bank-specific withdraw/deposit
	use.addChild(new RadialOption(RadialItem.BANK_TRANSFER));
	use.addChild(new RadialOption(RadialItem.BANK_ITEMS));
	if (creature.getBankBalance() > 0)
		use.addChild(new RadialOption(RadialItem.BANK_WITHDRAW_ALL));
	if (creature.getCashBalance() > 0)
		use.addChild(new RadialOption(RadialItem.BANK_DEPOSIT_ALL));
	// Galactic Reserve
	reserve = new RadialOption(RadialItem.BANK_RESERVE);
	if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
		reserve.addChild(new RadialOption(RadialItem.BANK_RESERVE_DEPOSIT));
	if (creature.getReserveBalance() > 0)
		reserve.addChild(new RadialOption(RadialItem.BANK_RESERVE_WITHDRAW));
	options.add(reserve);
};
var handleSelection = function(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
		case RadialItem.BANK_TRANSFER: {
			creature = player.getCreatureObject();
			SuiWindow = Java.type("resources.sui.SuiWindow");
			Trigger = Java.type("resources.sui.SuiWindow.Trigger");
			window = new SuiWindow("Script.transfer", player);
			window.setProperty('transaction.txtInputFrom:Text', 'From');
			window.setProperty('transaction.txtInputFrom:Text', 'From');
			window.setProperty('bg.caption.lblTitle:Text', '@base_player:bank_title');
			window.setProperty('Prompt.lblPrompt:Text', '@base_player:bank_prompt');
			window.setProperty('transaction.txtInputTo:Text', 'To');
			window.setProperty('transaction.lblFrom:Text', 'Cash');
			window.setProperty('transaction.lblTo:Text', 'Bank');
			window.setProperty('transaction.lblStartingFrom:Text', creature.getCashBalance());
			window.setProperty('transaction.lblStartingTo:Text', creature.getBankBalance());
			window.setProperty('transaction.txtInputFrom:Text', creature.getCashBalance());
			window.setProperty('transaction.txtInputTo:Text', creature.getBankBalance());
			window.setProperty('transaction:ConversionRatioFrom', '1');
			window.setProperty('transaction:ConversionRatioTo', '1');
			returnParams = new java.util.ArrayList();
			returnParams.add('transaction.txtInputFrom:Text');
			returnParams.add('transaction.txtInputTo:Text');
			window.addCallback(0, '', Trigger.OK, returnParams, "radial/terminal/bank");
			window.addCallback(1, '', Trigger.CANCEL, returnParams, "radial/terminal/bank");
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
				intentFactory.sendSystemMessage(player, '@error_message:bank_deposit')
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
				intentFactory.sendSystemMessage(player, '@error_message:bank_withdraw')
				break;
			}
			creature.setBankBalance(creature.getBankBalance() + amount);
			creature.setReserveBalance(creature.getReserveBalance() - amount);
			break;
		}
	}
};
var callback = function(player, creature, eventId, dataStrings) {
	switch (eventId) {
		case 0:
			creature.setCashBalance(Number(dataStrings[0]));
			creature.setBankBalance(Number(dataStrings[1]));
			intentFactory.sendSystemMessage(player, '@base_player:bank_success')
			break;
	}
};