function executeCommand(galacticManager, player, target, args) {
	var DanceIntent = Java.type("intents.DanceIntent");
	new DanceIntent(player.getCreatureObject()).broadcast();
}