import sys

#Args: name
def execute(objManager, player, target, args):
	ghost = player.getPlayerObject()
	if ghost is None:
		return
	
	waypoint = ghost.getWaypoint(target.getObjectId())
	if waypoint is None:
		return
	
	waypoint.setName(args)
	ghost.updateWaypoint(waypoint)
	return