function execute(galacticManager, player, target, args) {
    var ChatAvatarRequestIntent = Java.type("intents.chat.ChatAvatarRequestIntent");
    var RequestType = Java.type("intents.chat.ChatAvatarRequestIntent.RequestType");
    var name;

    if(args == null) {
        return;
    }

    name = args.split(" ")[0].toLowerCase(java.util.Locale.English);

    if(name == null) {
        return;
    }

    new ChatAvatarRequestIntent(player, name, RequestType.IGNORE_ADD_TARGET).broadcast();
}