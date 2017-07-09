import intents.chat.ChatBroadcastIntent
import resources.objects.SWGObject
import resources.player.AccessLevel
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (player.getAccessLevel() == AccessLevel.PLAYER) {
		new ChatBroadcastIntent(player, "Players cannot use this command :(").broadcast();
		return
	}

	def creatureObject = player.getCreatureObject()
	if (target) {
		creatureObject = target.getCreatureObject()
	}

	if (creatureObject.hasAbility("admin")) {
		creatureObject.removeAbility("admin")
		new ChatBroadcastIntent(player, "God Mode Disabled").broadcast();
	} else {
		creatureObject.addAbility("admin")
		new ChatBroadcastIntent(player, "God Mode Enabled").broadcast();
	}
}