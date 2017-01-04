function executeCommand(galacticManager, player, target, args) {
    var GroupEventIntent = Java.type("intents.GroupEventIntent");
    var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");
	var farAwayTarget;
	
	if (args) {
		farAwayTarget = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(args);
	}
	
	if (farAwayTarget) {
		new GroupEventIntent(GroupEventType.GROUP_INVITE, player, farAwayTarget.getCreatureObject()).broadcast();
	} else {
		new GroupEventIntent(GroupEventType.GROUP_INVITE, player, target).broadcast();
	}
}