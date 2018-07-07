package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.intents.gameplay.world.travel.TicketPurchaseIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdPurchaseTicket implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		String [] params = args.split(" ");
		if (params.length < 5)
			return;
		
		String destinationName = params[3].replaceAll("_", " ");
		
		new TicketPurchaseIntent(player.getCreatureObject(), params[2], destinationName, params[4].equals("1")).broadcast();
	}
	
}
