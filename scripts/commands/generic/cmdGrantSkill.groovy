import intents.experience.GrantSkillIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, args, target as CreatureObject, true).broadcast()
}