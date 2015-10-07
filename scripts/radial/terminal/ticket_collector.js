function getOptions(options) {
	options.add(new RadialOption(RadialItem.ITEM_USE));
	options.add(new RadialOption(RadialItem.EXAMINE));
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			Log.d("ticket_collector.js", "Ticket Collector Selection: ITEM_USE");
			var TicketUseIntent = Java.type("intents.travel.TicketUseIntent");
			
			new TicketUseIntent(player).broadcast();
			
			break;
	}
}