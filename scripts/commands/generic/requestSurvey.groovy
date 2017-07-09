import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import com.projectswg.common.debug.Log
import services.crafting.resource.galactic.storage.GalacticResourceContainer
import intents.crafting.survey.StartSurveyingIntent
import intents.chat.ChatBroadcastIntent

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args)
	if (resource == null) {
		new ChatBroadcastIntent(player, "Unknown resource: " + args).broadcast()
		return
	}
	if (player.getCreatureObject().hasAbility("admin")) {
		def spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, player.getCreatureObject().getTerrain());
		for (int i = 0; i < spawns.size(); i++) {
			new ChatBroadcastIntent(player, "Spawn: " + spawns.get(i)).broadcast()
		}
	}
	new StartSurveyingIntent(player.getCreatureObject(), resource).broadcast()
}
