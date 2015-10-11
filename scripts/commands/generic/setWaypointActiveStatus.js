function executeCommand(galacticManager, player, target, args) {
	var ghost = player.getPlayerObject();
	var waypoint;
	
	if(ghost == null || target == null) {
		return;
	}
	
	waypoint = ghost.getWaypoint(target.getObjectId());
	
	if(waypoint == null) {
		return;
	}
	
	if(waypoint.isActive()) {
		waypoint.setActive(false);
	} else {
		waypoint.setActive(true);
	}
	
	ghost.updateWaypoint(waypoint);
}