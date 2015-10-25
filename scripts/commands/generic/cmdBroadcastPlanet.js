function executeCommand(galacticManager, player, target, args) {
	intentFactory.broadcastPlanet(player.getCreatureObject().getLocation().getTerrain(), args);
}