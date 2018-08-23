package com.projectswg.holocore.intents.gameplay.world.travel.pet;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PetDeviceCallIntent extends Intent {
	
	private final Player player;
	private final IntangibleObject controlDevice;
	
	public PetDeviceCallIntent(@NotNull Player player, @NotNull IntangibleObject controlDevice) {
		this.player = player;
		this.controlDevice = controlDevice;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public IntangibleObject getControlDevice() {
		return controlDevice;
	}
	
	public static void broadcast(@NotNull Player player, @NotNull IntangibleObject controlDevice) {
		new PetDeviceCallIntent(player, controlDevice).broadcast();
	}
	
}
