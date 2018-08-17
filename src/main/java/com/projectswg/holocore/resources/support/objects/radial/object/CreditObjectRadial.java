package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootItemIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject;

import java.util.List;

public class CreditObjectRadial implements RadialHandlerInterface {
	
	public CreditObjectRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.ITEM_USE, "@space/space_loot:use_credit_chip"));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (target == null)
			return;
		assert target instanceof CreditObject;
		
		SWGObject lootInventory = target.getParent();
		assert lootInventory != null;
		
		SWGObject corpse = lootInventory.getParent();
		assert corpse instanceof AIObject;
		assert ((AIObject) corpse).getInventory() == lootInventory;
		
		LootItemIntent.broadcast(player.getCreatureObject(), (CreatureObject) corpse, target);
	}
	
}
