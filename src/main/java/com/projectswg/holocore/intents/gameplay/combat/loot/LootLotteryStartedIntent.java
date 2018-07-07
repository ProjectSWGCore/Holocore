package com.projectswg.holocore.intents.gameplay.combat.loot;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class LootLotteryStartedIntent extends Intent {
	
	private final @NotNull CreatureObject corpse;
	
	public LootLotteryStartedIntent(@NotNull CreatureObject corpse) {
		this.corpse = corpse;
	}
	
	@NotNull
	public CreatureObject getCorpse() {
		return corpse;
	}
	
	public static void broadcast(@NotNull CreatureObject corpse) {
		new LootLotteryStartedIntent(corpse).broadcast();
	}
}
