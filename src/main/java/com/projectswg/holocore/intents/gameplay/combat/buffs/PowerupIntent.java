package com.projectswg.holocore.intents.gameplay.combat.buffs;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PowerupIntent extends Intent {
	
	private final CreatureObject actor;
	private final TangibleObject powerupObject;
	
	public PowerupIntent(CreatureObject actor, TangibleObject powerupObject) {
		this.actor = actor;
		this.powerupObject = powerupObject;
	}
	
	public CreatureObject getActor() {
		return actor;
	}
	
	public TangibleObject getPowerupObject() {
		return powerupObject;
	}
	
	public static void broadcast(@NotNull CreatureObject actor, @NotNull TangibleObject powerupObject) {
		new PowerupIntent(actor, powerupObject).broadcast();
	}
}
