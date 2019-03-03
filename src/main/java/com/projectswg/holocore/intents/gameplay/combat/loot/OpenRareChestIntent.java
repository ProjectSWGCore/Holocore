package com.projectswg.holocore.intents.gameplay.combat.loot;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class OpenRareChestIntent extends Intent {
	
	private final CreatureObject actor;
	private final TangibleObject chest;
	
	public OpenRareChestIntent(CreatureObject actor, TangibleObject chest) {
		this.actor = actor;
		this.chest = chest;
	}
	
	public CreatureObject getActor() {
		return actor;
	}
	
	public TangibleObject getChest() {
		return chest;
	}
	
	public static void broadcast(@NotNull CreatureObject actor, @NotNull TangibleObject chest) {
		new OpenRareChestIntent(actor, chest).broadcast();
	}
	
}
