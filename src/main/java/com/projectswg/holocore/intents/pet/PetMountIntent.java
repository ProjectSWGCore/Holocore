package com.projectswg.holocore.intents.pet;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PetMountIntent extends Intent {
	
	private final Player player;
	private final CreatureObject pet;
	
	public PetMountIntent(@NotNull Player player, @NotNull CreatureObject pet) {
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
		new PetMountIntent(player, pet).broadcast();
	}
	
}
