from network.packets.swg.zone import ClientOpenContainerMessage

def execute(objManager, player, target, args):	
	player.sendPacket(ClientOpenContainerMessage(target.getObjectId()))
	return
	