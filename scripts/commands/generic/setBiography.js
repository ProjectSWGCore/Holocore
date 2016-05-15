function executeCommand(galacticManager, player, target, args) {
	
	if(args.length() > 1025) {
		return;
	}
	
	var BiographyUpdate = Java.type("network.packets.swg.zone.object_controller.BiographyUpdate");
	var creatureObject = player.getCreatureObject();
	var objectId = creatureObject.getObjectId();
	
	creatureObject.getPlayerObject().setBiography(args);
	player.sendPacket(new BiographyUpdate(objectId, objectId, args));
}
