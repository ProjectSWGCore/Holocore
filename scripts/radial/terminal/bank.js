var getOptions = function(options, player, target) {
	options.add(new RadialOption(0, RadialItem.ITEM_USE));
	options.add(new RadialOption(0, RadialItem.EXAMINE));
	options.add(new RadialOption(1, RadialItem.BANK_TRANSFER));
	options.add(new RadialOption(1, RadialItem.BANK_ITEMS));
};
var handleSelection = function(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
		case RadialItem.BANK_ITEMS:
			Log.d("bank.js", "Bank Selection: BANK_ITEMS");
			break;
		case RadialItem.BANK_TRANSFER:
			Log.d("bank.js", "Bank Selection: BANK_TRANSFER");
			break;
	}
};