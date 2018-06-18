package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.location.Location;
import me.joshlarson.jlcommon.utilities.Arguments;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;

public class AINavigationSupport {
	
	private AINavigationSupport() {
		
	}
	
	/**
	 * Returns a queue of locations to traverse on the path to within the min/max range of the specified destination, at the specified speed. If the source is already within range of the destination,
	 * this function returns an empty queue
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param minRange    the minimum range from target
	 * @param maxRange    the maximum range from target
	 * @param speed       the speed to travel at
	 * @return a queue of locations to travel
	 */
	@NotNull
	public static Queue<Location> navigateWithinRangeBounds(@NotNull Location source, @NotNull Location destination, double minRange, double maxRange, double speed) {
		Arguments.validate(minRange < maxRange, "min range must be less than max range");
		Arguments.validate(minRange >= 0, "min range must be greater than or equal to 0");
		Arguments.validate(!source.equals(destination), "source cannot be the same as the destination");
		
		double currentDistance = source.distanceTo(destination);
		if (currentDistance < minRange) {
			double requiredDistance = currentDistance + minRange;
			destination = interpolate(source, destination, requiredDistance / currentDistance);
		} else if (currentDistance > maxRange) {
			double requiredDistance = currentDistance - maxRange;
			destination = interpolate(source, destination, requiredDistance / currentDistance);
		} else {
			return new LinkedList<>();
		}
		return navigateTo(source, destination, speed);
	}
	
	/**
	 * Returns a queue of locations to traverse on the path to within range of the specified destination, at the specified speed If the source is already within range of the destination, this function
	 * returns an empty queue
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param range       the range to travel within
	 * @param speed       the speed to travel at
	 * @return a queue of locations to travel
	 */
	@NotNull
	public static Queue<Location> navigateWithinRange(@NotNull Location source, @NotNull Location destination, double range, double speed) {
		assert range >= 0 : "range should be positive";
		Arguments.validate(!source.equals(destination), "source cannot be the same as the destination");
		
		double currentDistance = source.distanceTo(destination);
		if (currentDistance > range) {
			double requiredDistance = currentDistance - range;
			destination = interpolate(source, destination, requiredDistance / currentDistance);
		} else {
			return new LinkedList<>();
		}
		return navigateTo(source, destination, speed);
	}
	
	/**
	 * Returns a queue of locations to traverse on the path to the specified destination, at the specified speed
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param speed       the speed to travel at
	 * @return a queue of locations to travel
	 */
	public static Queue<Location> navigateTo(@NotNull Location source, @NotNull Location destination, double speed) {
		speed -= 0.5; // Makes the animation more smooth on the client
		Queue<Location> path = new LinkedList<>();
		double totalDistance = source.distanceTo(destination);
		for (double currentDistance = speed; currentDistance <= totalDistance; currentDistance += speed) {
			path.offer(interpolate(source, destination, currentDistance / totalDistance));
		}
		return path;
	}
	
	/**
	 * Returns the next location on the path to destination, with the given speed, unless source is within the target range
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param minRange    the minimum range from target
	 * @param maxRange    the maximum range from target
	 * @param speed       the speed to travel at
	 * @return the next location to move to
	 */
	public static Location getNextStepTo(@NotNull Location source, @NotNull Location destination, double minRange, double maxRange, double speed) {
		double currentDistance = source.distanceTo(destination);
		if (currentDistance < minRange)
			return interpolate(source, destination, Math.min(speed, currentDistance - minRange) / currentDistance);
		if (currentDistance > maxRange)
			return interpolate(source, destination, Math.min(speed, currentDistance - maxRange) / currentDistance);
		return source;
	}
	
	/**
	 * Returns the next location on the path to destination, with the given speed, up until the given range
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param range       the range to travel within 
	 * @param speed       the speed to travel at
	 * @return the next location to move to
	 */
	public static Location getNextStepTo(@NotNull Location source, @NotNull Location destination, double range, double speed) {
		double currentDistance = source.distanceTo(destination);
		if (currentDistance <= range)
			return source;
		return interpolate(source, destination, Math.min(speed, currentDistance - range) / currentDistance);
	}
	
	/**
	 * Returns the next location on the path to destination, with the given speed
	 *
	 * @param source      the source location
	 * @param destination the destination location
	 * @param speed       the speed to travel at
	 * @return the next location to move to
	 */
	public static Location getNextStepTo(@NotNull Location source, @NotNull Location destination, double speed) {
		double currentDistance = source.distanceTo(destination);
		if (currentDistance <= speed)
			return destination;
		return interpolate(source, destination, speed / currentDistance);
	}
	
	private static Location interpolate(Location l1, Location l2, double percentage) {
		return Location.builder()
				.setTerrain(l1.getTerrain())
				.setX(l1.getX() + (l2.getX()-l1.getX())*percentage)
				.setY(l1.getY() + (l2.getY()-l1.getY())*percentage)
				.setZ(l1.getZ() + (l2.getZ()-l1.getZ())*percentage)
				.setHeading(Math.toDegrees(Math.atan2(l2.getX()-l1.getX(), l2.getZ()-l1.getZ())))
				.build();
	}
	
	public enum Speed {
		WALK (1.4),
		RUN  (7.3);
		
		private final double speed;
		
		Speed(double speed) {
			this.speed = speed;
		}
		
		public double getSpeed() {
			return speed;
		}
		
	}
	
}
