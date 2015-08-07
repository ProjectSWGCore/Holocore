var execute = function(galManager, player, target, args) {
	var ChatBroadcastIntent = Java.type("intents.chat.ChatBroadcastIntent");
	var BroadcastType = Java.type("intents.chat.ChatBroadcastIntent.BroadcastType");
	new ChatBroadcastIntent(args, player, player.getCreatureObject().getLocation().getTerrain(), BroadcastType.PLANET).broadcast();
};