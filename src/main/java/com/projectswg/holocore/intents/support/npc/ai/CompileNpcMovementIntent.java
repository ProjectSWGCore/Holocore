package com.projectswg.holocore.intents.support.npc.ai;

import com.projectswg.holocore.resources.support.npc.ai.NavigationOffset;
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint;
import com.projectswg.holocore.resources.support.npc.ai.NavigationRouteType;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompileNpcMovementIntent extends Intent {
	
	private final AIObject obj;
	private final List<NavigationPoint> points;
	private final NavigationRouteType type;
	private final double speed;
	private final NavigationOffset offset;
	
	public CompileNpcMovementIntent(@NotNull AIObject obj, @NotNull List<NavigationPoint> points, @NotNull NavigationRouteType type, double speed) {
		this(obj, points, type, speed, null);
	}
	
	public CompileNpcMovementIntent(@NotNull AIObject obj, @NotNull List<NavigationPoint> points, @NotNull NavigationRouteType type, double speed, @Nullable NavigationOffset offset) {
		this.obj = obj;
		this.points = new ArrayList<>(points);
		this.type = type;
		this.speed = speed;
		this.offset = offset;
	}
	
	@NotNull
	public AIObject getObject() {
		return obj;
	}
	
	@NotNull
	public List<NavigationPoint> getPoints() {
		return Collections.unmodifiableList(points);
	}
	
	@NotNull
	public NavigationRouteType getType() {
		return type;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	@Nullable
	public NavigationOffset getOffset() {
		return offset;
	}
	
	public static void broadcast(@NotNull AIObject obj, @NotNull List<NavigationPoint> points, @NotNull NavigationRouteType type, double speed) {
		new CompileNpcMovementIntent(obj, points, type, speed).broadcast();
	}
	
	public static void broadcast(@NotNull AIObject obj, @NotNull List<NavigationPoint> points, @NotNull NavigationRouteType type, double speed, @Nullable NavigationOffset offset) {
		new CompileNpcMovementIntent(obj, points, type, speed, offset).broadcast();
	}
	
}
