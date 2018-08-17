package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;

import java.util.List;

public class AIObjectRadial implements RadialHandlerInterface {
	
	public AIObjectRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		AIObject ai = (AIObject) target;
		
		if (ai.getPosture() != Posture.DEAD) {
			return;
		}
		
		options.add(RadialOption.create(RadialItem.LOOT,
				RadialOption.create(RadialItem.LOOT)));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		AIObject ai = (AIObject) target;
		
		if (ai.getPosture() != Posture.DEAD) {
			return;
		}
		
		switch (selection) {
			case LOOT:
				LootRequestIntent.broadcast(player, ai, LootRequestIntent.LootType.LOOT);
				break;
			case LOOT_ALL:
				LootRequestIntent.broadcast(player, ai, LootRequestIntent.LootType.LOOT_ALL);
				break;
		}
	}
	
}
