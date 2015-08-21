var execute = function(galManager, player, target, args) {
	var ChatAvatarRequestIntent = Java.type("intents.chat.ChatAvatarRequestIntent");
	var RequestType = Java.type("intents.chat.ChatAvatarRequestIntent.RequestType");
	new ChatAvatarRequestIntent(player, null, RequestType.FRIEND_LIST).broadcast();
};