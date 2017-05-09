import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    if(args == null)
        return

    def name = args.split(" ").toLowerCase(Locale.English)

    if(name == null)
        return

    new ChatAvatarRequestIntent(player, name, RequestType.IGNORE_ADD_TARGET).broadcast()
}