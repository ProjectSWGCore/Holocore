var execute = function(objManager, player, target, args) {
	if(args.contains(" ")) {
		return;
	}
	
	player.getCreatureObject().setMoodId(args);
}