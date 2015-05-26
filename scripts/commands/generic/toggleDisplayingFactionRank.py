import sys
from resources.player.PlayerFlags import FACTIONRANK

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(FACTIONRANK);
	return