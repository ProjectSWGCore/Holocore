import sys
from resources.player.PlayerFlags import LFW

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(LFW);
	return