package com.projectswg.holocore.intents.gameplay.world.travel.pet;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PetDeviceCallIntent extends Intent {
	
	private final CreatureObject creature;
	private final IntangibleObject controlDevice;
	
	public PetDeviceCallIntent(@NotNull CreatureObject creature, @NotNull IntangibleObject controlDevice) {
		this.creature = creature;
		this.controlDevice = controlDevice;
	}
	
	@NotNull
	public CreatureObject getCreature() {
		return creature;
	}
	
	@NotNull
	public IntangibleObject getControlDevice() {
		return controlDevice;
	}
	
	public static void broadcast(@NotNull CreatureObject creature, @NotNull IntangibleObject controlDevice) {
		new PetDeviceCallIntent(creature, controlDevice).broadcast();
	}
	
}
