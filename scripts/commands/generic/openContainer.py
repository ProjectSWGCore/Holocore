import sys
from network.packets.swg.zone import OpenedContainerMessage

def execute(objManager, player, target, args):	
	player.sendPacket(OpenedContainerMessage(target.getObjectId()))
	return
	