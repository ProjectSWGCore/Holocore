function executeCommand(galacticManager, player, target, args) {

	var DuelPlayerIntent = Java.type("intents.combat.DuelPlayerIntent");
	var DuelEventType = Java.type("intents.combat.DuelPlayerIntent.DuelEventType");
	
	if (player.getCreatureObject().hasSentDuelRequestToPlayer(target) && !player.getCreatureObject().isDuelingPlayer(target)) {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.CANCEL).broadcast();
	} else if (target.hasSentDuelRequestToPlayer(player.getCreatureObject()) && !player.getCreatureObject().hasSentDuelRequestToPlayer(target)) {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.DECLINE).broadcast();
	} else {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.END).broadcast();
	}
}