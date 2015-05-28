import sys
from resources.player.PlayerFlags import HELPER

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(HELPER);
	return