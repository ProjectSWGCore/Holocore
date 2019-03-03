package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.combat.buffs.PowerupIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.OpenRareChestIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

import java.util.Collection;

public class PowerupRadial extends SWGObjectRadial {
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.ITEM_USE, "@spam:powerup_use"));
		options.add(RadialOption.createSilent(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case ITEM_USE:
				PowerupIntent.broadcast(player.getCreatureObject(), (TangibleObject) target);
				break;
		}
	}
}
