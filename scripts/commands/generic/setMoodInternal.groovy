import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (args.contains(" ")) {
		return
	}

	player.getCreatureObject().setMoodId(Byte.valueOf(args))
}