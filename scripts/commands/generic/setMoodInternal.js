var execute = function(galManager, player, target, args) {
	if(args.contains(" ")) {
		return;
	}
	
	player.getCreatureObject().setMoodId(args);
};