package com.projectswg.holocore.services.support.npc.ai;

import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent;
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent;
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint;
import com.projectswg.holocore.resources.support.npc.ai.NavigationRouteType;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AIMovementService extends Service {
	
	private final Map<AIObject, NavigationRoute> routes;
	private final ScheduledThreadPool movementThreads;
	
	public AIMovementService() {
		this.routes = new ConcurrentHashMap<>();
		this.movementThreads = new ScheduledThreadPool(Runtime.getRuntime().availableProcessors(), "ai-movement-service-%d");
	}
	
	@Override
	public boolean start() {
		movementThreads.start();
		movementThreads.executeWithFixedRate(1000, 1000, this::executeRoutes);
		return true;
	}
	
	@Override
	public boolean stop() {
		movementThreads.stop();
		return movementThreads.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleStartNpcMovementIntent(StartNpcMovementIntent snmi) {
		AIObject obj = snmi.getObject();
		
		List<NavigationPoint> route = NavigationPoint.from(obj.getParent(), obj.getLocation(), snmi.getParent(), snmi.getDestination(), snmi.getSpeed());
		if (route.isEmpty())
			routes.remove(obj);
		else
			routes.put(obj, new NavigationRoute(obj, route, NavigationRouteType.TERMINATE));
	}
	
	@IntentHandler
	private void handleCompileNpcMovementIntent(CompileNpcMovementIntent snmi) {
		AIObject obj = snmi.getObject();
		List<NavigationPoint> route = new ArrayList<>(snmi.getPoints().size());
		for (NavigationPoint point : snmi.getPoints()) {
			appendRoutePoint(route, point, snmi.getSpeed());
		}
		if (snmi.getType() == NavigationRouteType.LOOP && !snmi.getPoints().isEmpty())
			appendRoutePoint(route, snmi.getPoints().get(0), snmi.getSpeed());
		
		if (route.isEmpty())
			routes.remove(obj);
		else
			routes.put(obj, new NavigationRoute(obj, route, snmi.getType()));
	}
	
	@IntentHandler
	private void handleStopNpcMovementIntent(StopNpcMovementIntent snmi) {
		routes.remove(snmi.getObject());
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject corpse = cki.getCorpse();
		if (corpse instanceof AIObject)
			routes.remove(corpse);
	}
	
	private void executeRoutes() {
		routes.values().forEach(NavigationRoute::execute);
	}
	
	private void appendRoutePoint(List<NavigationPoint> waypoints, NavigationPoint waypoint, double speed) {
		NavigationPoint prev = waypoints.isEmpty() ? null : waypoints.get(waypoints.size()-1);
		if (waypoint.isNoOperation()) {
			waypoints.add(waypoint);
			return;
		}
		if (prev == null) {
			waypoints.add(NavigationPoint.at(waypoint.getParent(), waypoint.getLocation(), speed));
		} else {
			if (prev.getLocation().equals(waypoint.getLocation()) && prev.getParent() == waypoint.getParent())
				return;
			waypoints.addAll(NavigationPoint.from(prev.getParent(), prev.getLocation(), waypoint.getParent(), waypoint.getLocation(), speed));
		}
	}
	
	private static class NavigationRoute {
		
		private final AIObject obj;
		private final List<NavigationPoint> route;
		private final NavigationRouteType type;
		private final AtomicInteger index;
		
		public NavigationRoute(AIObject obj, List<NavigationPoint> route, NavigationRouteType type) {
			this.obj = obj;
			this.route = route;
			this.type = type;
			this.index = new AtomicInteger(0);
		}
		
		public void execute() {
			int index = this.index.getAndIncrement();
			if (index >= route.size()) {
				switch (type) {
					case LOOP:
						this.index.set(0);
						index = 0;
						break;
					case TERMINATE:
						StopNpcMovementIntent.broadcast(obj);
						return;
				}
			}
			assert index < route.size() && index >= 0;
			
			route.get(index).move(obj);
		}
	}
	
}
