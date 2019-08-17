package com.projectswg.holocore.resources.support.global.commands.callbacks.loot;

import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.resources.gameplay.combat.loot.LootType;
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
		
		LootRequestIntent.broadcast(player, (CreatureObject) target, LootType.LOOT_ALL);
	}
	
}
