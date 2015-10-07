function executeCommand = function(galacticManager, player, target, args) {
	var TicketPurchaseIntent = Java.type("intents.travel.TicketPurchaseIntent");
	params = args.split(" ");
	
	destinationPlanet = params[2];
	destinationName = params[3].replaceAll("_", " ");
	roundTrip = params[4].equals("1");
	
	new TicketPurchaseIntent(player.getCreatureObject(), destinationPlanet, destinationName, roundTrip).broadcast();
}
