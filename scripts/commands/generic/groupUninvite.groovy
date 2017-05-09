import intents.GroupEventIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_UNINVITE, player, target as CreatureObject).broadcast()
}
