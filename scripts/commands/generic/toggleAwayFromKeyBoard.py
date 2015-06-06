import sys
from resources.player.PlayerFlags import AFK

def execute(objManager, player, target, args):
	player.getPlayerObject().toggleFlag(PlayerFlags.AFK)
	return