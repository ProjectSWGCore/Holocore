package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.resources.gameplay.combat.loot.LootType;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.log.Log;

import java.util.Collection;

public class AIObjectRadial implements RadialHandlerInterface {
	
	public AIObjectRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		AIObject ai = (AIObject) target;
		
		if (ai.getPosture() != Posture.DEAD) {
			return;
		}
		
		options.add(RadialOption.create(RadialItem.LOOT, RadialOption.create(RadialItem.LOOT_ALL)));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		AIObject ai = (AIObject) target;
		
		if (ai.getPosture() != Posture.DEAD) {
			return;
		}
		
		switch (selection) {
			case LOOT:
				LootRequestIntent.broadcast(player, ai, LootType.LOOT);
				break;
			case LOOT_ALL:
				LootRequestIntent.broadcast(player, ai, LootType.LOOT_ALL);
				break;
		}
	}
	
}
