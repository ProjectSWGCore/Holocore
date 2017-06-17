import intents.GroupEventIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (target != null && (args == null || args.isEmpty())) {
		new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_MAKE_LEADER, player, target as CreatureObject).broadcast()
	}
}
