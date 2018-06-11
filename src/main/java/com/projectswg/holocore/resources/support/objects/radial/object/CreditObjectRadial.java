package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public class CreditObjectRadial implements RadialHandlerInterface {
	
	public CreditObjectRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		options.add(new RadialOption(RadialItem.TRANSFER_CREDITS_TO_BANK_ACCOUNT));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		LootRequestIntent.broadcast(player, target, LootRequestIntent.LootType.CREDITS);
	}
	
}
