import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {

	if(player.getPlayerObject() == null || target == null) {
		return
	}
	
	def waypoint = player.getPlayerObject().getWaypoint(target.getObjectId())
	
	if(waypoint == null) {
		return
	}
	
	if(waypoint.isActive()) {
		waypoint.setActive(false)
	} else {
		waypoint.setActive(true)
	}
	
	player.getPlayerObject().updateWaypoint(waypoint)
}