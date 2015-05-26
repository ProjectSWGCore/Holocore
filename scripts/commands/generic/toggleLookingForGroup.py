import sys
from resources.player.PlayerFlags import LFG

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(LFG);
	return