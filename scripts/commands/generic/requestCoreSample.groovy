import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import com.projectswg.common.debug.Log
import services.crafting.resource.galactic.storage.GalacticResourceContainer
import intents.crafting.survey.SampleResourceIntent
import intents.chat.SystemMessageIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args)
	if (resource == null) {
		SystemMessageIntent.broadcastPersonal(player, "Unknown resource: " + args)
		return
	}
	new SampleResourceIntent(player.getCreatureObject(), resource).broadcast()
}
