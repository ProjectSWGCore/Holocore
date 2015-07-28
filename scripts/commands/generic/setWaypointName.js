var execute = function(objManager, player, target, args) {
	var ghost = player.getPlayerObject();
	var waypoint;
	
	if(ghost == null || target == null) {
		return;
	}
	
	waypoint = ghost.getWaypoint(target.getObjectId());
	
	if(waypoint == null) {
		return;
	}
	
	waypoint.setName(args);
	
	ghost.updateWaypoint(waypoint);
}