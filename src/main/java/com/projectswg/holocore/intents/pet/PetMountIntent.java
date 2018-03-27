package com.projectswg.holocore.intents.pet;

import com.projectswg.common.control.Intent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;

import javax.annotation.Nonnull;

public class PetMountIntent extends Intent {
	
	private Player player;
	private CreatureObject pet;
	
	private PetMountIntent(Player player, CreatureObject pet) {
		this.player = player;
		this.pet = pet;
	}
	
	@Nonnull
	public Player getPlayer() {
		return player;
	}
	
	@Nonnull
	public CreatureObject getPet() {
		return pet;
	}
	
	public static void broadcast(@Nonnull Player player, @Nonnull CreatureObject pet) {
		new PetMountIntent(player, pet).broadcast();
	}
	
}
