import intents.GroupEventIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager
import utilities.IntentFactory

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def farAwayTarget
	
	if (args) {
		farAwayTarget = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(args)
	}
	
	if (farAwayTarget != null) {
		new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_INVITE, player, (farAwayTarget as Player).getCreatureObject()).broadcast()
	} else {
		if (target instanceof CreatureObject) {
			new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_INVITE, player, target).broadcast()
		} else {
			IntentFactory.sendSystemMessage(player, "@group:invite_no_target_self")
		}
	}
}