import intents.DanceIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	new DanceIntent(player.getCreatureObject()).broadcast()
}