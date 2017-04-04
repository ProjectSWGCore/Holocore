function executeCommand(galacticManager, player, target, args) {

	var DuelPlayerIntent = Java.type("intents.combat.DuelPlayerIntent");
	var DuelEventType = Java.type("intents.combat.DuelPlayerIntent.DuelEventType");
	
	if (player.getCreatureObject().sentDuelRequestToPlayer(target) && !player.getCreatureObject().isInDuelWithPlayer(target)) {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.CANCEL).broadcast();
	} else if (target.getSentDuelRequestToPlayer(player.getCreatureObject()) && !player.getCreatureObject.sentDuelRequestToPlayer(target)) {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.DECLINE).broadcast();
	} else {
		new DuelPlayerIntent(player.getCreatureObject(), target, DuelEventType.END).broadcast();
	}
}