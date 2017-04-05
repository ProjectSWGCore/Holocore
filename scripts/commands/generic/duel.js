function executeCommand(galacticManager, player, target, args) {

	var DuelPlayerIntent = Java.type("intents.combat.DuelPlayerIntent");
	var DuelEventType = Java.type("intents.combat.DuelPlayerIntent.DuelEventType");
	
	if (target.hasSentDuelRequestToPlayer(player.getCreatureObject())) {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.ACCEPT).broadcast();
	} else {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.REQUEST).broadcast();
	}
}