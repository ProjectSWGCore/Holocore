import intents.chat.SpatialChatIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def cmdArgs = args.split(' ', 5)

    new SpatialChatIntent(player, Integer.valueOf(cmdArgs[1]), args.substring(10), Integer.valueOf(cmdArgs[2]))
			.broadcast()
}