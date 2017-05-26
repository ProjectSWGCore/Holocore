import intents.chat.ChatAvatarRequestIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	new ChatAvatarRequestIntent(player, null, ChatAvatarRequestIntent.RequestType.FRIEND_LIST).broadcast()
}