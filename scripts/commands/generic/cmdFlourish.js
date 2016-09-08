function executeCommand(galacticManager, player, target, args) {
	
	// No performance, no flourish!
	if(!player.getCreatureObject().isPerforming()) {
		intentFactory.sendSystemMessage(player, "@performance:flourish_not_performing");
		return;
	}
	
	switch(args) {
		case "1":
		case "2":
		case "3":
		case "4":
		case "5":
		case "6":
		case "7":
		case "8":
			flourishName = "skill_action_" + args;
			break;
		case "9":
			flourishName = "mistake";
			break;
		default:
			intentFactory.sendSystemMessage(player, "@performance:flourish_not_valid");
			return;
	}
	
	var FlourishIntent = Java.type("intents.FlourishIntent");
	new FlourishIntent(player, flourishName).broadcast();
}
