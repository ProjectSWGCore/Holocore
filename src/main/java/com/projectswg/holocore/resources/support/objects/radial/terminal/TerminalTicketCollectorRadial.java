package com.projectswg.holocore.resources.support.objects.radial.terminal;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.world.travel.TicketUseIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public class TerminalTicketCollectorRadial implements RadialHandlerInterface {
	
	public TerminalTicketCollectorRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		options.add(new RadialOption(RadialItem.ITEM_USE));
		options.add(new RadialOption(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case ITEM_USE:
				new TicketUseIntent(player).broadcast();
				break;
		}
	}
	
}
