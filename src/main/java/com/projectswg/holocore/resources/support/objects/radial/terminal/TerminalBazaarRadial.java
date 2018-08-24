package com.projectswg.holocore.resources.support.objects.radial.terminal;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.Collection;

public class TerminalBazaarRadial implements RadialHandlerInterface {
	
	public TerminalBazaarRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.ITEM_USE));
		options.add(RadialOption.createSilent(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		
	}
	
}
