var getOptions = function(options) {
	options.add(new RadialOption(0, RadialItem.ITEM_USE));
	options.add(new RadialOption(0, RadialItem.EXAMINE));
};
var handleSelection = function(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			Log.d("bazaar.js", "Bazaar Selection: ITEM_USE");
			break;
	}
};