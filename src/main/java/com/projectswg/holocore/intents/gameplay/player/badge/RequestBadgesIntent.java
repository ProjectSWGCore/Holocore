package com.projectswg.holocore.intents.gameplay.player.badge;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.control.Intent;

public class RequestBadgesIntent extends Intent {
	
	private final Player requester;
	private final SWGObject target;
	
	public RequestBadgesIntent(Player requester, SWGObject target) {
		this.requester = requester;
		this.target = target;
	}
	
	public Player getRequester() {
		return requester;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
}
