import intents.WatchIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (target === null) {
		return
	}

	if (player.getCreatureObject().equals(target)) {
		// You can't watch yourself, silly!
		return
	}

	new WatchIntent(player.getCreatureObject(), target, true).broadcast()
}