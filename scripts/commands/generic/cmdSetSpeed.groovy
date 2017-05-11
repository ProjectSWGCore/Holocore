import resources.objects.SWGObject
import resources.player.AccessLevel
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def creature = player.getCreatureObject()

	if (player.getAccessLevel() == AccessLevel.PLAYER) {
		print("Error: Your Accesslevel is to low to use that command")
		return
	}

	if (creature == null) {
		print("Error: No Player or CreatureObject")
		return
	}

	creature.setMovementScale(Integer.valueOf(args))
}
