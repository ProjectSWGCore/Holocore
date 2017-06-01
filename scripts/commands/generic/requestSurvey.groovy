import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import com.projectswg.common.debug.Log
import services.crafting.resource.galactic.storage.GalacticResourceContainer
import intents.crafting.survey.StartSurveyingIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args)
	new StartSurveyingIntent(player.getCreatureObject(), resource).broadcast()
}
