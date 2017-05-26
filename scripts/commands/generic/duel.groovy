import intents.combat.DuelPlayerIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def creoTarget = target as CreatureObject

	if (creoTarget.hasSentDuelRequestToPlayer(player.getCreatureObject())) {
		new DuelPlayerIntent(player.getCreatureObject(), creoTarget, DuelPlayerIntent.DuelEventType.ACCEPT).broadcast()
	} else {
		new DuelPlayerIntent(player.getCreatureObject(), creoTarget, DuelPlayerIntent.DuelEventType.REQUEST).broadcast()
	}
}