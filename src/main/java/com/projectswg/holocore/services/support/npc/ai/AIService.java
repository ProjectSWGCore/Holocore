package com.projectswg.holocore.services.support.npc.ai;

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Collection<AIObject> aiObjects;
	private final AtomicBoolean started;
	
	public AIService() {
		this.executor = new ScheduledThreadPool(16, "ai-service-%d");
		this.aiObjects = ConcurrentHashMap.newKeySet();
		this.started = new AtomicBoolean(false);
	}
	
	@Override
	public boolean start() {
		executor.start();
		started.set(true);
		for (AIObject obj : aiObjects) {
			obj.start(executor);
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		started.set(false);
		aiObjects.clear();
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		if (!(oci.getObject() instanceof AIObject))
			return;
		AIObject obj = (AIObject) oci.getObject();
		if (aiObjects.add(obj) && started.get())
			obj.start(executor);
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		if (!(doi.getObject() instanceof AIObject))
			return;
		AIObject obj = (AIObject) doi.getObject();
		if (aiObjects.remove(obj) && started.get())
			obj.stop();
	}
	
	@IntentHandler
	private void handleEnterCombatIntent(EnterCombatIntent eci) {
		if (!(eci.getSource() instanceof AIObject))
			return;
		AIObject obj = (AIObject) eci.getSource();
		obj.startCombatMode();
	}
	
}
