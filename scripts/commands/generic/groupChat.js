function executeCommand(galacticManager, player, target, args) {
	if(args.isEmpty()) {
		// Should there for some reason be no actual message to display, do nothing
		return;
	}
	
	var groupObjectId = player.getCreatureObject().getGroupId();
	
	if(groupObjectId == 0) {
		// If their group object ID is 0, they're ungrouped. Ungrouped players can't send group messages.
		return;
	}
	
	var CoreManager = Java.type("services.CoreManager");
	var galaxy = CoreManager.getGalaxy().getName();
	var groupChatPath = "SWG." + galaxy + ".group." + groupObjectId + ".GroupChat";
	
	var ChatRoomUpdateIntent = Java.type("intents.chat.ChatRoomUpdateIntent");
	var UpdateType = Java.type("intents.chat.ChatRoomUpdateIntent.UpdateType");
	
	new ChatRoomUpdateIntent(player, groupChatPath, groupObjectId, null,
					args, UpdateType.SEND_MESSAGE, false).broadcast();
}