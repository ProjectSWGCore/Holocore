import sys
from resources.player import AccessLevel

def execute(objManager, player, target, args):
	ghost = player.getCreatureObject()
	
	if player.getAccessLevel() == AccessLevel.PLAYER:
		print("Error: Your Accesslevel is to low to use that command")
		return	
	
	if ghost is None:
		print("Error: No Player or CreatureObject")
		return
		
	ghost.setMovementScale(float(args))
	return