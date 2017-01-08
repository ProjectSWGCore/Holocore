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
		if (target instanceof Java.type("resources.objects.creature.CreatureObject")) {
			new GroupEventIntent(GroupEventType.GROUP_INVITE, player, target).broadcast();
		} else {
			intentFactory.sendSystemMessage(player, "@group:invite_no_target_self");
		}
	}
}