function executeCommand(galacticManager, player, target, args) {
	var DeathblowIntent = Java.type("intents.combat.DeathblowIntent");
	new DeathblowIntent(player.getCreatureObject(), target).broadcast();
}