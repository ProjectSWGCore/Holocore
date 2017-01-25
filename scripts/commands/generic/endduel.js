function executeCommand(galacticManager, player, target, args) {

	var DuelPlayerIntent = Java.type("intents.combat.DuelPlayerIntent");
	
	new DuelPlayerIntent(player.getCreatureObject(), target, true).broadcast();
}