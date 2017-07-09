import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import intents.chat.ChatBroadcastIntent;

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	new ChatBroadcastIntent(args, player, player.getCreatureObject().getTerrain(), ChatBroadcastIntent.BroadcastType.AREA).broadcast();
}