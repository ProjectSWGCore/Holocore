import resources.objects.SWGObject
import resources.player.Player
import resources.player.PlayerFlags
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	player.getPlayerObject().toggleFlag(PlayerFlags.LFG)
}