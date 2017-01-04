function executeCommand(galacticManager, player, target, args) {
    var GroupEventIntent = Java.type("intents.GroupEventIntent");
    var GroupEventType = Java.type("intents.GroupEventIntent.GroupEventType");

	if (target) {
		new GroupEventIntent(GroupEventType.GROUP_INVITE, player, target).broadcast();
	} else if (args) {
		var farAwayTarget = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(args);
		
		if (farAwayTarget) {
			new GroupEventIntent(GroupEventType.GROUP_INVITE, player, farAwayTarget.getCreatureObject()).broadcast();
		}
	}
}