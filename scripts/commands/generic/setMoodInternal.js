function executeCommand(galacticManager, player, target, args) {
	if(args.contains(" ")) {
		return;
	}
	
	player.getCreatureObject().setMoodId(args);
}