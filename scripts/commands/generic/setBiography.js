function executeCommand(galacticManager, player, target, args) {
	var BiographyUpdate = Java.type("network.packets.swg.zone.object_controller.BiographyUpdate");
	var objectId = player.getCreatureObject().getObjectId();
	
	player.sendPacket(new BiographyUpdate(objectId, objectId, args));
}
