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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.projectswg.common.data.location.Terrain;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import services.objects.ObjectCreator;

public class TestBuildoutObjectList {
	
	private static final TestBuildoutObjectList INSTANCE = new TestBuildoutObjectList();
	
	private final List<SWGObject> mosEisleyObjects;
	private final AtomicBoolean loaded;
	
	public static TestBuildoutObjectList getInstance() {
		INSTANCE.load();
		return INSTANCE;
	}
	
	private TestBuildoutObjectList() {
		this.mosEisleyObjects = new ArrayList<>(2524);
		this.loaded = new AtomicBoolean(false);
	}
	
	private void load() {
		if (loaded.getAndSet(true))
			return;
		double maxAngle = 2 * Math.PI;
		for (double radius = 10; radius <= 250; radius += 20) {
			for (double angle = 0; angle < maxAngle; angle += maxAngle / 10) {
				SWGObject object = ObjectCreator.createObjectFromTemplate("object/building/tatooine/shared_housing_tatt_style01_med.iff");
				object.setPosition(Terrain.TATOOINE, radius * Math.cos(angle) + 3500, 5, radius * Math.sin(angle) - 4800);
				mosEisleyObjects.add(object);
			}
		}
//		ClientBuildoutService buildoutService = new ClientBuildoutService();
//		mosEisleyObjects.addAll(buildoutService.loadClientObjectsByArea(843).values().stream().filter(obj -> obj.getParent() != null).collect(Collectors.toList()));
	}
	
	private boolean isValidWithinRange(SWGObject obj, SWGObject inRange, double range) {
		if (obj.equals(inRange))
			return false;
		if (inRange instanceof CreatureObject && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		return isWithinRange(obj, inRange, range);
	}
	
	private boolean isWithinRange(SWGObject a, SWGObject b, double range) {
		return square(a.getX()-b.getX()) + square(a.getZ()-b.getZ()) <= square(Math.max(b.getLoadRange(), range));
	}
	
	private double square(double x) {
		return x * x;
	}
	
	public List<SWGObject> getMosEisleyObjects() {
		return Collections.unmodifiableList(mosEisleyObjects);
	}
	
	public List<SWGObject> getWithinRangeObjects(CreatureObject creature) {
		double loadRange = creature.getLoadRange();
		return mosEisleyObjects.stream().filter(obj -> isValidWithinRange(creature, obj, loadRange)).collect(Collectors.toList());
	}
	
}
