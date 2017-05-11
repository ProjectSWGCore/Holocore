import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import utilities.IntentFactory

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	IntentFactory.broadcastGalaxy(args, player)
}