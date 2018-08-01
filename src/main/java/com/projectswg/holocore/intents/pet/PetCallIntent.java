package com.projectswg.holocore.intents.pet;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PetCallIntent extends Intent {
	
	private final Player player;
	private final SWGObject controlDevice;
	
	public PetCallIntent(@NotNull Player player, @NotNull SWGObject controlDevice) {
		this.player = player;
		this.controlDevice = controlDevice;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public SWGObject getControlDevice() {
		return controlDevice;
	}
	
	public static void broadcast(@NotNull Player player, @NotNull SWGObject controlDevice) {
		new PetCallIntent(player, controlDevice).broadcast();
	}
	
}
