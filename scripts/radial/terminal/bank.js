var getOptions = function(options, player, target) {
	options.add(new RadialOption(0, RadialItem.ITEM_USE));
	options.add(new RadialOption(0, RadialItem.EXAMINE));
	options.add(new RadialOption(1, RadialItem.BANK_TRANSFER));
	options.add(new RadialOption(1, RadialItem.BANK_ITEMS));
	creature = player.getCreatureObject();
	if (creature.getBankBalance() > 0)
		options.add(new RadialOption(1, RadialItem.BANK_WITHDRAW_ALL));
	if (creature.getCashBalance() > 0)
		options.add(new RadialOption(1, RadialItem.BANK_DEPOSIT_ALL));
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
			intentFactory.sendSystemMessage(player, "You successfully withdraw " + amount + " credits from your account.");
			break;
		}
		case RadialItem.BANK_DEPOSIT_ALL: {
			creature = player.getCreatureObject();
			amount = creature.getCashBalance();
			creature.setBankBalance(amount + creature.getBankBalance());
			creature.setCashBalance(0);
			intentFactory.sendSystemMessage(player, "You successfully deposit " + amount + " credits to your account.");
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