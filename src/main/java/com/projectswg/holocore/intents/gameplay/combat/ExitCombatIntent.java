package com.projectswg.holocore.intents.gameplay.combat;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class ExitCombatIntent extends Intent {
	
	private final CreatureObject source;
	
	public ExitCombatIntent(@NotNull CreatureObject source) {
		this.source = source;
	}
	
	@NotNull
	public CreatureObject getSource() {
		return source;
	}
	
	public static void broadcast(@NotNull CreatureObject source) {
		new ExitCombatIntent(source).broadcast();
	}
	
}
