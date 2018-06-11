package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public class UsableObjectRadial extends SWGObjectRadial {
	
	public UsableObjectRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		super.getOptions(options, player, target);
		options.add(new RadialOption(RadialItem.ITEM_USE));
		options.add(new RadialOption(RadialItem.EXAMINE));
	}
	
}
