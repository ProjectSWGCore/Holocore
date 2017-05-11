import intents.chat.ChatRoomUpdateIntent
import resources.objects.SWGObject
import resources.player.Player
import services.CoreManager
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	if (args.isEmpty()) {
		// Should there for some reason be no actual message to display, do nothing
		return
	}

	def groupObjectId = player.getCreatureObject().getGroupId()

	if (groupObjectId == 0) {
		// If their group object ID is 0, they're ungrouped. Ungrouped players can't send group messages.
		return
	}

	def galaxy = CoreManager.getGalaxy().getName()
	def groupChatPath = "SWG." + galaxy + ".group." + groupObjectId + ".GroupChat"

	new ChatRoomUpdateIntent(player, groupChatPath, groupObjectId as String, null, args,
			ChatRoomUpdateIntent.UpdateType.SEND_MESSAGE, false).broadcast()
}