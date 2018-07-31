package com.projectswg.holocore.intents.support.npc.ai;

import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScheduleNpcModeIntent extends Intent {
	
	private final AIObject obj;
	private final NpcMode mode;
	
	public ScheduleNpcModeIntent(@NotNull AIObject obj, @Nullable NpcMode mode) {
		this.obj = obj;
		this.mode = mode;
	}
	
	@NotNull
	public AIObject getObject() {
		return obj;
	}
	
	@Nullable
	public NpcMode getMode() {
		return mode;
	}
	
	public static void broadcast(@NotNull AIObject obj, @Nullable NpcMode mode) {
		new ScheduleNpcModeIntent(obj, mode).broadcast();
	}
	
}
