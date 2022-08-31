package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NavigationPoint {
	
	private final SWGObject parent;
	private final Location location;
	private final double speed;
	private final int hash;
	
	private NavigationPoint(SWGObject parent, Location location, double speed) {
		this.parent = parent;
		this.location = location;
		this.speed = speed;
		this.hash = Objects.hash(parent, location);
	}
	
	public SWGObject getParent() {
		return parent;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public void move(SWGObject obj) {
		if (isNoOperation())
			return;
		if (parent == null)
			obj.broadcast(new MoveObjectIntent(obj, location, speed));
		else
			obj.broadcast(new MoveObjectIntent(obj, parent, location, speed));
	}
	
	public boolean isNoOperation() {
		return location == null || speed == 0;
	}
	
	public double distanceTo(SWGObject otherParent, Location otherLocation) {
		Location myLocation = location;
		if (parent != null)
			myLocation = Location.builder(myLocation).translateLocation(parent.getWorldLocation()).build();
		if (otherParent != null)
			otherLocation = Location.builder(otherLocation).translateLocation(otherParent.getWorldLocation()).build();
		return myLocation.distanceTo(otherLocation);
	}
	
	public double distanceTo(NavigationPoint point) {
		return distanceTo(point.getParent(), point.getLocation());
	}
	
	public double distanceTo(SWGObject obj) {
		return distanceTo(obj.getParent(), obj.getLocation());
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NavigationPoint))
			return false;
		NavigationPoint point = (NavigationPoint) o;
		return hash == point.hash && parent == point.parent && Objects.equals(location, point.location);
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public String toString() {
		return String.format("NavigationPoint[%s @ %s]", parent, location.getPosition());
	}
	
	public static List<NavigationPoint> nop(NavigationPoint prev, int intervals) {
		List<NavigationPoint> nop = new ArrayList<>();
		for (int i = 0; i < intervals; i++)
			nop.add(new NavigationPoint(prev.getParent(), prev.getLocation(), 0));
		return nop;
	}
	
	public static NavigationPoint at(@Nullable SWGObject parent, @NotNull Location location, double speed) {
		return new NavigationPoint(parent, location, speed);
	}
	
	public static List<NavigationPoint> from(@Nullable SWGObject sourceParent, @NotNull Location source, @Nullable SWGObject destinationParent, @NotNull Location destination, double speed) {
		assert sourceParent == null || sourceParent instanceof CellObject;
		assert destinationParent == null || destinationParent instanceof CellObject;
		List<Portal> route = getBuildingRoute((CellObject) sourceParent, (CellObject) destinationParent, source, destination);
		if (route == null)
			return new ArrayList<>();
		List<NavigationPoint> points = createIntraBuildingRoute(route, (CellObject) sourceParent, source, speed);
		if (!route.isEmpty())
			source = destinationParent == null ? buildWorldPortalLocation(route.get(route.size() - 1)) : buildPortalLocation(route.get(route.size() - 1));
		points.addAll(from(destinationParent, source, destination, speed));
		return points;
	}
	
	/**
	 * Returns a list of locations to traverse on the path to the specified destination, at the specified speed
	 *
	 * @param parent      the parent for each point
	 * @param source      the source location
	 * @param destination the destination location
	 * @param speed       the speed to travel at
	 * @return a queue of locations to travel
	 */
	public static List<NavigationPoint> from(@Nullable SWGObject parent, @NotNull Location source, @NotNull Location destination, double speed) {
		speed = Math.floor(speed);
		double totalDistance = source.distanceTo(destination);
		List<NavigationPoint> path = new ArrayList<>();
		
		double currentDistance = speed;
		while (currentDistance < totalDistance) {
			path.add(interpolate(parent, source, destination, speed, currentDistance / totalDistance));
			currentDistance += speed;
		}
		path.add(interpolate(parent, source, destination, speed, 1));
		return path;
	}
	
	private static NavigationPoint interpolate(SWGObject parent, Location l1, Location l2, double speed, double percentage) {
		double heading = Math.toDegrees(Math.atan2(l2.getX()-l1.getX(), l2.getZ()-l1.getZ()));
		if (percentage <= 0)
			return new NavigationPoint(parent, Location.builder(l1)
					.setY(parent == null ? DataLoader.Companion.terrains().getHeight(l1) : l1.getY())
					.setHeading(heading)
					.build(), speed);
		if (percentage >= 1)
			return new NavigationPoint(parent, Location.builder(l2)
					.setY(parent == null ? DataLoader.Companion.terrains().getHeight(l2) : l2.getY())
					.setHeading(heading)
					.build(), speed);
		
		double interpX = l1.getX() + (l2.getX()-l1.getX())*percentage;
		double interpY = l1.getY() + (l2.getY()-l1.getY())*percentage;
		double interpZ = l1.getZ() + (l2.getZ()-l1.getZ())*percentage;
		return new NavigationPoint(parent, Location.builder()
				.setTerrain(l1.getTerrain())
				.setX(interpX)
				.setY(parent == null ? DataLoader.Companion.terrains().getHeight(l1.getTerrain(), interpX, interpZ) : interpY)
				.setZ(interpZ)
				.setHeading(heading)
				.build(), speed);
	}
	
	private static List<NavigationPoint> createIntraBuildingRoute(List<Portal> route, CellObject from, Location start, double speed) {
		List<NavigationPoint> points = new ArrayList<>();
		for (Portal portal : route) {
			if (from == null)
				points.addAll(from(null, start, buildWorldPortalLocation(portal), speed));
			else
				points.addAll(from(from, start, buildPortalLocation(portal), speed));
			from = portal.getOtherCell(from);
			start = buildPortalLocation(portal);
		}
		return points;
	}
	
	private static List<Portal> getBuildingRoute(CellObject from, CellObject to, Location start, Location destination) {
		if (from == to)
			return List.of();
		if (from != null) {
			for (Portal fromPortal : from.getPortals()) {
				if (fromPortal.getOtherCell(from) == to)
					return List.of(fromPortal);
			}
		}
		
		PriorityQueue<NavigationRouteNode> nodes = new PriorityQueue<>(getNearbyPortals(new NavigationRouteNode(new ArrayList<>(), from, start, destination), to, start, destination));
		while (!nodes.isEmpty()) {
			NavigationRouteNode node = nodes.poll();
			assert node != null : "loop precondition";
			
			if (node.getNode() == to)
				return node.getRoute();
			
			nodes.addAll(getNearbyPortals(node, to, start, destination));
		}
		return null;
	}
	
	private static List<NavigationRouteNode> getNearbyPortals(NavigationRouteNode node, CellObject to, Location start, Location destination) {
		List<NavigationRouteNode> nearby = new ArrayList<>();
		if (node.getNode() == null) {
			BuildingObject building = (BuildingObject) to.getParent();
			assert building != null;
			for (Portal portal : building.getPortals()) {
				if (node.getRoute().contains(portal))
					continue;
				if (portal.getCell1() != null && portal.getCell2() != null)
					continue;
				
				nearby.add(new NavigationRouteNode(appendToCopy(node.getRoute(), portal), portal.getOtherCell(null), start, destination));
			}
		} else {
			for (Portal portal : node.getNode().getPortals()) {
				if (node.getRoute().contains(portal))
					continue;
				nearby.add(new NavigationRouteNode(appendToCopy(node.getRoute(), portal), portal.getOtherCell(node.getNode()), start, destination));
			}
		}
		return nearby;
	}
	
	private static <T> List<T> appendToCopy(List<T> original, T newElement) {
		List<T> replacement = new ArrayList<>(original);
		replacement.add(newElement);
		return replacement;
	}
	
	private static Location buildWorldPortalLocation(Portal portal) {
		SWGObject building = portal.getCell1().getParent();
		assert building instanceof BuildingObject;
		return Location.builder(buildPortalLocation(portal)).translateLocation(building.getLocation()).build();
	}
	
	private static Location buildPortalLocation(Portal portal) {
		return Location.builder()
				.setX(average(portal.getFrame1().getX(), portal.getFrame2().getX()))
				.setY(average(portal.getFrame1().getY(), portal.getFrame2().getY()))
				.setZ(average(portal.getFrame1().getZ(), portal.getFrame2().getZ()))
				.setTerrain(portal.getCell1().getTerrain())
				.build();
	}
	
	private static double average(double x, double y) {
		return (x+y) / 2;
	}
	
	private static class NavigationRouteNode implements Comparable<NavigationRouteNode> {
		
		private final List<Portal> route;
		private final CellObject node;
		private final double cost;
		
		public NavigationRouteNode(List<Portal> route, CellObject node, Location start, Location destination) {
			this.route = route;
			this.node = node;
			double cost = 0;
			for (Portal portal : route) {
				Location portalLocation = buildWorldPortalLocation(portal);
				cost += portalLocation.distanceTo(start);
				start = portalLocation;
			}
			this.cost = cost + start.distanceTo(destination);
		}
		
		public List<Portal> getRoute() {
			return Collections.unmodifiableList(route);
		}
		
		public CellObject getNode() {
			return node;
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof NavigationRouteNode && ((NavigationRouteNode) o).node == node;
		}
		
		@Override
		public int hashCode() {
			return node.hashCode();
		}
		
		@Override
		public int compareTo(@NotNull NavigationPoint.NavigationRouteNode o) {
			return Double.compare(cost, o.cost);
		}
		
	}
	
}
