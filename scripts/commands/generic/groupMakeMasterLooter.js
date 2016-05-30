function executeCommand(galacticManager, player, target, args) {
	
    var GroupEventIntent = Java.type("intents.GroupEventIntent");
    var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");
	
	new GroupEventIntent(GroupEventType.GROUP_MAKE_MASTER_LOOTER, player, target).broadcast();
}
