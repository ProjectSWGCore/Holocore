import sys
from resources.player.PlayerFlags import ROLEPLAYER

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(ROLEPLAYER);
	return