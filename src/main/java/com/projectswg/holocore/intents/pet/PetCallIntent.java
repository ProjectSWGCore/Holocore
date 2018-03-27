package com.projectswg.holocore.intents.pet;

import com.projectswg.common.control.Intent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;

import javax.annotation.Nonnull;

public class PetCallIntent extends Intent {
	
	private Player player;
	private SWGObject controlDevice;
	
	private PetCallIntent(Player player, SWGObject controlDevice) {
		this.player = player;
		this.controlDevice = controlDevice;
	}
	
	@Nonnull
	public Player getPlayer() {
		return player;
	}
	
	@Nonnull
	public SWGObject getControlDevice() {
		return controlDevice;
	}
	
	public static void broadcast(@Nonnull Player player, @Nonnull SWGObject controlDevice) {
		new PetCallIntent(player, controlDevice).broadcast();
	}
	
}
