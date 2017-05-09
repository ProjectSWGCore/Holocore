import intents.combat.DeathblowIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    new DeathblowIntent(player.getCreatureObject(), target as CreatureObject).broadcast()
}