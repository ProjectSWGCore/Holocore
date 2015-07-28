var execute = function(objManager, player, target, args) {
	var ChatBroadcastIntent = Java.type("intents.chat.ChatBroadcastIntent");
	var BroadcastType = Java.type("intents.chat.ChatBroadcastIntent.BroadcastType");
	new ChatBroadcastIntent(args, player, player.getCreatureObject().getLocation().getTerrain(), BroadcastType.GALAXY).broadcast();
}