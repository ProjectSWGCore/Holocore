var execute = function(objManager, player, target, args) {
	var ClientOpenContainerMessage = Java.type("network.packets.swg.zone.ClientOpenContainerMessage");
	player.sendPacket(new ClientOpenContainerMessage(target.getObjectId()));
}