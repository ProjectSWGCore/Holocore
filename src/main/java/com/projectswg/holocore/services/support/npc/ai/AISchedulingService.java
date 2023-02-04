package com.projectswg.holocore.services.support.npc.ai;

import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent;
import com.projectswg.holocore.resources.support.npc.ai.NpcCombatMode;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AISchedulingService extends Service {
	
	private final Map<AIObject, NpcMode> modes;
	
	public AISchedulingService() {
		this.modes = new ConcurrentHashMap<>();
	}
	
	@IntentHandler
	private void handleScheduleNpcModeIntent(ScheduleNpcModeIntent snmi) {
		AIObject obj = snmi.getObject();
		NpcMode next = snmi.getMode();
		if (next == null)
			next = obj.getDefaultMode();
		
		NpcMode prev = next == null ? modes.remove(obj) : modes.put(obj, next);
		if (prev == next)
			return;
		
		stop(obj, prev);
		start(obj, next);
	}
	
	@IntentHandler
	private void handleStartNpcCombatIntent(StartNpcCombatIntent snci) {
		modes.compute(snci.getObject(), (o, prev) -> {
			if (prev instanceof NpcCombatMode) {
				((NpcCombatMode) prev).addTargets(snci.getTargets());
				return prev;
			}
			NpcCombatMode mode = new NpcCombatMode(o);
			mode.addTargets(snci.getTargets());
			start(o, mode);
			return mode;
		});
	}
	
	private void start(@NotNull AIObject obj, @Nullable NpcMode mode) {
		if (mode == null)
			return;
		
		obj.setActiveMode(mode);
	}
	
	private void stop(@NotNull AIObject obj, @Nullable NpcMode mode) {
		if (mode == null)
			return;
		
		obj.setActiveMode(null);
	}
	
}
