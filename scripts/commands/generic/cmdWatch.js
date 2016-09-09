function executeCommand(galacticManager, player, target, args) {
	var actor = player.getCreatureObject();
	
	if(target === null) {
		return;
	}
	
	if(actor.equals(target)) {
		// You can't watch yourself, silly!
		return;
	}
	
    var WatchIntent = Java.type("intents.WatchIntent");
    new WatchIntent(actor, target, true).broadcast();
}