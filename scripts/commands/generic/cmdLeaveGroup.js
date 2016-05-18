function executeCommand(galacticManager, player, target, args) {
	var GroupEventIntent = Java.type("intents.GroupEventIntent");
	var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");

	if (target != null && args == null || args.length == 0) {
		new GroupEventIntent(GroupEventType.GROUP_LEAVE, player, target).broadcast();
	}
}