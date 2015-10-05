function executeCommand(galacticManager, player, target, args) {
    var GroupEventIntent = Java.type("intents.GroupEventIntent");
    var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");

    intentFactory.sendSystemMessage("Group: " + args);

    new GroupEventIntent(GroupEventType.GROUP_JOIN, player).broadcast();
}