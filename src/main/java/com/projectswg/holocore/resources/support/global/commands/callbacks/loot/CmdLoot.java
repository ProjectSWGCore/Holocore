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
		if (!(target instanceof CreatureObject))
			return;
		
		LootRequestIntent.broadcast(player, (CreatureObject) target, LootType.LOOT);
	}
	
}
