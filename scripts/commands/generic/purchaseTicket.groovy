import intents.travel.TicketPurchaseIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def params = args.split(" ")

	def destinationName = params[3].replaceAll("_", " ")

	new TicketPurchaseIntent(player.getCreatureObject(), params[2], destinationName, params[4] == "1").broadcast()
}
