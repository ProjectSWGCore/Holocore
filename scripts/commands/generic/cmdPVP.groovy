import intents.FactionIntent
import resources.PvpFaction
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import utilities.IntentFactory

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def creature = player.getCreatureObject()
	def intent = null
	
	// Ziggy was using this to test until recruiters are enabled.

	if (args.length > 0) {
		if (args.indexOf("imperial") > -1) {
			intent = new FactionIntent(creature, PvpFaction.IMPERIAL)
		}
		else if (args.indexOf("rebel") > -1) {
			intent = new FactionIntent(creature, PvpFaction.REBEL)
		}
		else {
			intent = new FactionIntent(creature, PvpFaction.NEUTRAL)
		}
		intent.broadcast()
	} else if (creature.getPvpFaction() != PvpFaction.NEUTRAL) {
		intent = new FactionIntent(creature, FactionIntent.FactionIntentType.SWITCHUPDATE)
		intent.broadcast()
	} else {
		IntentFactory.sendSystemMessage(player, "@faction_recruiter:not_aligned")
	}
}