import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import intents.chat.SystemMessageIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	SystemMessageIntent.broadcastArea(player, args)
}