function executeCommand(galacticManager, player, target, args) {
	if (player.getAccessLevel() == (Java.type('resources.player.AccessLevel')).PLAYER) {
		intentFactory.sendSystemMessage(player, "Unable to access /tip command - currently reserved for admins");
		return;
	}
	argSplit = args.split(" ");
	if (argSplit.length < 2) {
		intentFactory.sendSystemMessage(player, "Invalid Arguments: " + args);
		return;
	}
	creature = player.getCreatureObject();
	if (argSplit[0] == "bank")
		creature.setBankBalance(creature.getBankBalance() + Number(argSplit[1]));
	else if (argSplit[0] == "cash")
		creature.setCashBalance(creature.getCashBalance() + Number(argSplit[1]));
	else
		intentFactory.sendSystemMessage(player, "Unknown Destination: " + argSplit[0]);
}