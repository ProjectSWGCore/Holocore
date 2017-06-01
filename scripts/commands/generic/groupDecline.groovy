import intents.GroupEventIntent
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_DECLINE, player).broadcast()
}