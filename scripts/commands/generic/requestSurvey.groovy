import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import com.projectswg.common.debug.Log
import services.crafting.resource.galactic.storage.GalacticResourceContainer
import intents.crafting.survey.StartSurveyingIntent
import intents.chat.SystemMessageIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args)
	if (resource == null) {
		SystemMessageIntent.broadcastPersonal(player, "Unknown resource: " + args)
		return
	}
	if (player.getCreatureObject().hasAbility("admin")) {
		def spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, player.getCreatureObject().getTerrain());
		for (int i = 0; i < spawns.size(); i++) {
			SystemMessageIntent.broadcastPersonal(player, "Spawn: " + spawns.get(i))
		}
	}
	new StartSurveyingIntent(player.getCreatureObject(), resource).broadcast()
}
