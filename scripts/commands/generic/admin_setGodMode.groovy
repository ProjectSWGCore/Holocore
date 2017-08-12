import intents.chat.SystemMessageIntent
import resources.objects.SWGObject
import resources.player.AccessLevel
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (player.getAccessLevel() == AccessLevel.PLAYER) {
		SystemMessageIntent.broadcastPersonal(player, "Players cannot use this command :(")
		return
	}

	def creatureObject = player.getCreatureObject()
	if (target) {
		creatureObject = target.getCreatureObject()
	}

	if (creatureObject.hasAbility("admin")) {
		creatureObject.removeAbility("admin")
		SystemMessageIntent.broadcastPersonal(player, "God Mode Disabled")
	} else {
		creatureObject.addAbility("admin")
		SystemMessageIntent.broadcastPersonal(player, "God Mode Enabled")
	}
}