package com.projectswg.holocore.resources.support.global.commands.callbacks.loot;

import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent.LootType;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public class CmdLoot implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		if (!(target instanceof CreatureObject) || creature == null)
			return;
		
		CreatureObject targetCreature = (CreatureObject) target;
		SWGObject targetInventory = targetCreature.getSlottedObject("inventory");
		if (targetInventory == null)
			return;
		
		if (creature.isContainerOpen(targetInventory, "") || args.trim().equals("all"))
			LootRequestIntent.broadcast(player, (CreatureObject) target, LootType.LOOT_ALL);
		else
			LootRequestIntent.broadcast(player, (CreatureObject) target, LootType.LOOT);
	}
	
}
