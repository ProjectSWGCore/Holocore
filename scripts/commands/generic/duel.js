function executeCommand(galacticManager, player, target, args) {

	var DuelPlayerIntent = Java.type("intents.combat.DuelPlayerIntent");
	var DuelEventType = Java.type("intents.combat.DuelPlayerIntent.DuelEventType");
	
	new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.REQUEST).broadcast();
}