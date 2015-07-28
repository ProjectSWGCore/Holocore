var execute = function(objManager, player, target, args) {
	var creature = player.getCreatureObject();
	var AccessLevel = Java.type("resources.player.AccessLevel");
	
	if(player.getAccessLevel() == AccessLevel.PLAYER) {
		print("Error: Your Accesslevel is to low to use that command");
		return;
	}
	
	if(creature == null) {
		print("Error: No Player or CreatureObject");
		return;
	}
	
	creature.setMovementScale(args);
}
