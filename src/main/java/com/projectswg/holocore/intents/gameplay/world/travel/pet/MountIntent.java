package com.projectswg.holocore.intents.gameplay.world.travel.pet;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class MountIntent extends Intent {
	
	private final CreatureObject creature;
	private final CreatureObject pet;
	
	public MountIntent(@NotNull CreatureObject creature, @NotNull CreatureObject pet) {
		this.creature = creature;
		this.pet = pet;
	}
	
	@NotNull
	public CreatureObject getCreature() {
		return creature;
	}
	
	@NotNull
	public CreatureObject getPet() {
		return pet;
	}
	
	public static void broadcast(@NotNull CreatureObject creature, @NotNull CreatureObject pet) {
		new MountIntent(creature, pet).broadcast();
	}
	
}
