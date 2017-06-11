import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import intents.chat.ChatAvatarRequestIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (args == null)
		return

	def name = args.toLowerCase(Locale.ENGLISH)

	if (name == null)
		return

	new ChatAvatarRequestIntent(player, name, ChatAvatarRequestIntent.RequestType.IGNORE_ADD_TARGET).broadcast()
}