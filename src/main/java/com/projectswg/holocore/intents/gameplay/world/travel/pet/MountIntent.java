package com.projectswg.holocore.intents.gameplay.world.travel.pet;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class MountIntent extends Intent {
	
	private final Player player;
	private final CreatureObject pet;
	
	public MountIntent(@NotNull Player player, @NotNull CreatureObject pet) {
		this.player = player;
		this.pet = pet;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public CreatureObject getPet() {
		return pet;
	}
	
	public static void broadcast(@NotNull Player player, @NotNull CreatureObject pet) {
		new MountIntent(player, pet).broadcast();
	}
	
}
