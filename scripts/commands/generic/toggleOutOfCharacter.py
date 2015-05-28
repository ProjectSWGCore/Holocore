import sys
from resources.player.PlayerFlags import OOC

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(OOC);
	return