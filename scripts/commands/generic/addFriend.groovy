import intents.chat.ChatAvatarRequestIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    if (args == null)
        return

    def name = args.split(" ")[0].toLowerCase(Locale.ENGLISH)
    if (name == null)
        return

    new ChatAvatarRequestIntent(player, name, ChatAvatarRequestIntent.RequestType.FRIEND_ADD_TARGET).broadcast()
}