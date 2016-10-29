/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.objects.awareness;

import resources.Location;
import resources.buildout.BuildoutArea;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import network.packets.swg.zone.UpdateTransformMessage;
import network.packets.swg.zone.UpdateTransformWithParentMessage;

public class DataTransformHandler {
	
	private final SpeedCheckHandler speedCheckHandler;
	
	public DataTransformHandler() {
		speedCheckHandler = new SpeedCheckHandler();
	}
	
	public boolean handleMove(SWGObject obj, Location requestedLocation, double speed, int update) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			speedCheckHandler.moveObjectSpeedChecks((CreatureObject) obj, requestedLocation);
		BuildoutArea area = obj.getBuildoutArea();
		if (area != null)
			requestedLocation = area.adjustLocation(requestedLocation);
		obj.sendObservers(createTransform(obj, requestedLocation, speed, update));
		return true;
	}
	
	public boolean handleMove(SWGObject obj, SWGObject parent, Location requestedLocation, double speed, int update) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			speedCheckHandler.moveObjectSpeedChecks((CreatureObject) obj, parent, requestedLocation);
		obj.sendObservers(createTransform(obj, parent.getObjectId(), requestedLocation, speed, update));
		return true;
	}
	
	private UpdateTransformMessage createTransform(SWGObject obj, Location requestedLocation, double speed, int update) {
		UpdateTransformMessage transform = new UpdateTransformMessage();
		transform.setObjectId(obj.getObjectId());
		transform.setX((short) (requestedLocation.getX() * 4 + 0.5));
		transform.setY((short) (requestedLocation.getY() * 4 + 0.5));
		transform.setZ((short) (requestedLocation.getZ() * 4 + 0.5));
		transform.setUpdateCounter(obj.getNextUpdateCount());
		transform.setDirection(getMovementAngle(requestedLocation));
		transform.setSpeed((byte) (speed+0.5));
		transform.setLookAtYaw((byte) 0);
		transform.setUseLookAtYaw(false);
		return transform;
	}
	
	private UpdateTransformWithParentMessage createTransform(SWGObject obj, long cellId, Location requestedLocation, double speed, int update) {
		UpdateTransformWithParentMessage transform = new UpdateTransformWithParentMessage(cellId, obj.getObjectId());
		transform.setLocation(requestedLocation);
		transform.setUpdateCounter(obj.getNextUpdateCount());
		transform.setDirection(getMovementAngle(requestedLocation));
		transform.setSpeed((byte) (speed + 0.5));
		transform.setLookDirection((byte) 0); // lookAtYaw * 16
		transform.setUseLookDirection(false);
		return transform;
	}
	
	private byte getMovementAngle(Location requestedLocation) {
		byte movementAngle = (byte) 0.0f;
		double wOrient = requestedLocation.getOrientationW();
		double yOrient = requestedLocation.getOrientationY();
		double sq = Math.sqrt(1 - (wOrient*wOrient));
		
		if (sq != 0) {
			if (requestedLocation.getOrientationW() > 0 && requestedLocation.getOrientationY() < 0) {
				wOrient *= -1;
				yOrient *= -1;
			}
			movementAngle = (byte) ((yOrient / sq) * (2 * Math.acos(wOrient) / 0.06283f));
		}
		
		return movementAngle;
	}
	
}
