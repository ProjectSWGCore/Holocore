import intents.combat.DuelPlayerIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {

	if (player.getCreatureObject().hasSentDuelRequestToPlayer(target as CreatureObject) && !player.getCreatureObject().isDuelingPlayer(target as CreatureObject)) {
		new DuelPlayerIntent(player.getCreatureObject(), target as CreatureObject, DuelPlayerIntent.DuelEventType.CANCEL).broadcast()
	} else if (target.hasSentDuelRequestToPlayer(player.getCreatureObject()) && !player.getCreatureObject().hasSentDuelRequestToPlayer(target as CreatureObject)) {
		new DuelPlayerIntent(player.getCreatureObject(), target as CreatureObject, DuelPlayerIntent.DuelEventType.DECLINE).broadcast()
	} else {
		new DuelPlayerIntent(player.getCreatureObject(), target as CreatureObject, DuelPlayerIntent.DuelEventType.END).broadcast()
	}
}