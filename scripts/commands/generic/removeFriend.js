var execute = function(objManager, player, target, args) {
	var ChatAvatarRequestIntent = Java.type("intents.chat.ChatAvatarRequestIntent");
	var RequestType = Java.type("intents.chat.ChatAvatarRequestIntent.RequestType");
	var ProsePackage = Java.type("resources.encodables.ProsePackage");
	var ChatBroadcastIntent = Java.type("intents.chat.ChatBroadcastIntent");
	var ghost = player.getPlayerObject();
	var name;
	
	if(ghost == null || args == null) {
		return;
	}
	
	name = args.split(" ")[0].toLowerCase(java.util.Locale.English);
	
	if(name == null) {
		return;
	}
	
	if(!ghost.getFriendsList().contains(name)) {
		new ChatBroadcastIntent(player, new ProsePackage("@cmnty:friend_not_found", "TT", name)).broadcast();
		return;
	}
	
	new ChatAvatarRequestIntent(player, name, RequestType.FRIEND_REMOVE_TARGET).broadcast();
}