function getOptions(options, player, target) {
	var use = new RadialOption(RadialItem.ITEM_USE);
	options.add(use);
	options.get(0).setOverriddenText("@collection:consume_item");
}
function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.USE:
			Log.d("bazaar.js", "Bazaar Selection: ITEM_USE");
			break;
	}
}
