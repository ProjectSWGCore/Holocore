package com.projectswg.holocore.intents.combat;

import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class EnterCombatIntent extends Intent {
	
	private final CreatureObject source;
	private final CreatureObject target;
	
	public EnterCombatIntent(@NotNull CreatureObject source, @NotNull CreatureObject target) {
		this.source = source;
		this.target = target;
	}
	
	@NotNull
	public CreatureObject getSource() {
		return source;
	}
	
	@NotNull
	public CreatureObject getTarget() {
		return target;
	}
	
	public static void broadcast(@NotNull CreatureObject source, @NotNull CreatureObject target) {
		new EnterCombatIntent(source, target).broadcast();
	}
	
}
