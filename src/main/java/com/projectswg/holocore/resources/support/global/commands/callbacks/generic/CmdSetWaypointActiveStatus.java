package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSetWaypointActiveStatus implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (player.getPlayerObject() == null || target == null) {
			return;
		}
		
		WaypointObject waypoint = player.getPlayerObject().getWaypoint(target.getObjectId());
		
		if (waypoint == null) {
			return;
		}
		
		waypoint.setActive(!waypoint.isActive());
		
		player.getPlayerObject().updateWaypoint(waypoint);
	}
	
}
