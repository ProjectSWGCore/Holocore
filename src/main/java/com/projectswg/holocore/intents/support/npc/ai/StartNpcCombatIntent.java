package com.projectswg.holocore.intents.support.npc.ai;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StartNpcCombatIntent extends Intent {
	
	private final AIObject obj;
	private final List<CreatureObject> targets;
	
	public StartNpcCombatIntent(@NotNull AIObject obj, @NotNull Collection<CreatureObject> targets) {
		this.obj = obj;
		this.targets = new ArrayList<>(targets);
	}
	
	@NotNull
	public AIObject getObject() {
		return obj;
	}
	
	@NotNull
	public Collection<CreatureObject> getTargets() {
		return Collections.unmodifiableCollection(targets);
	}
	
	public static void broadcast(@NotNull AIObject obj, @NotNull Collection<CreatureObject> targets) {
		new StartNpcCombatIntent(obj, targets).broadcast();
	}
	
}
