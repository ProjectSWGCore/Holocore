import intents.travel.TicketUseIntent
import resources.objects.SWGObject
import resources.player.Player
import resources.radial.RadialItem
import resources.radial.RadialOption

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	options.add(new RadialOption(RadialItem.ITEM_USE))
	options.add(new RadialOption(RadialItem.EXAMINE))
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			new TicketUseIntent(player).broadcast()
			break
	}
}