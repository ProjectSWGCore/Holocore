function execute(galManager, player, target, args) {
	var cmdArgs = args.split(' ', 5);
	var SpatialChatIntent = Java.type("intents.chat.SpatialChatIntent");
	new SpatialChatIntent(player, cmdArgs[1], args.substring(cmdArgs.toString().length()), cmdArgs[2]).broadcast();
};