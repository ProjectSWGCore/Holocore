import sys

#Args: color planet x y z
def execute(objManager, player, target, args):
	print(args)
	cmdArgs = args.split(" ", 4)
	
	waypoint = objManager.createObject("object/waypoint/shared_waypoint.iff")
	waypoint.getLocation().setTerrain(player.getCreatureObject().getLocation().getTerrain())
	waypoint.setLocation(float(cmdArgs[2]), float(cmdArgs[3]), float(cmdArgs[4]))
	waypoint.setActive(True)
	waypoint.setName("New Waypoint")
	
	player.getPlayerObject().addWaypoint(waypoint)
	return