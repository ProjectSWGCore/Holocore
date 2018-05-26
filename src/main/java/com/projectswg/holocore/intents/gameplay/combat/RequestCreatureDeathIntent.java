package com.projectswg.holocore.intents.gameplay.combat;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class RequestCreatureDeathIntent extends Intent {
	
	private final CreatureObject corpse;
	private final CreatureObject killer;
	
	public RequestCreatureDeathIntent(@NotNull CreatureObject corpse, @NotNull CreatureObject killer) {
		this.corpse = corpse;
		this.killer = killer;
	}
	
	@NotNull
	public CreatureObject getCorpse() {
		return corpse;
	}
	
	@NotNull
	public CreatureObject getKiller() {
		return killer;
	}
	
	public static void broadcast(@NotNull CreatureObject corpse, @NotNull CreatureObject killer) {
		new RequestCreatureDeathIntent(corpse, killer).broadcast();
	}
	
}
