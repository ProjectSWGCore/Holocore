function executeCommand(galacticManager, player, target, args) {
    var GroupEventIntent = Java.type("intents.GroupEventIntent");
    var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");

    if (target != null && (args == null || args.length == 0)) {
        new GroupEventIntent(GroupEventType.GROUP_DISBAND, player, target).broadcast();
    } else {
        var kickingPlayer = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(args);
        if (kickingPlayer != null)
            new GroupEventIntent(GroupEventType.GROUP_DISBAND, player, kickingPlayer.getCreatureObject()).broadcast();
        else
            new GroupEventIntent(GroupEventType.GROUP_DISBAND, player, null).broadcast();
    }
}