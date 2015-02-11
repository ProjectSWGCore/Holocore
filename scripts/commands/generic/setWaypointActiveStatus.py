import sys

def execute(objManager, player, target, args):
	ghost = player.getPlayerObject()
	if ghost is None:
		return
	
	waypoint = ghost.getWaypoint(target.getObjectId())
	if waypoint is None:
		return
	
	if waypoint.isActive():
		waypoint.setActive(False)
	else:
		waypoint.setActive(True)
	
	ghost.updateWaypoint(waypoint)
	return