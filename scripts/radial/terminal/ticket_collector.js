function getOptions(options) {
	options.add(new RadialOption(0, RadialItem.ITEM_USE));
	options.add(new RadialOption(0, RadialItem.EXAMINE));
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			Log.d("ticket_collector.js", "Ticket Collector Selection: ITEM_USE");
			break;
	}
};