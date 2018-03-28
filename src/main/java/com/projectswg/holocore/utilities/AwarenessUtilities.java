/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.utilities;

import java.util.Set;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;

public class AwarenessUtilities {
	
	public static void handleUpdateAwarenessManual(SWGObject obj, Set<SWGObject> oldAware, Set<Player> oldObservers, Set<SWGObject> newAware, Set<Player> newObservers) {
//		callForNewObserver(oldObservers, newObservers, obj::createObject);
//		callForOldObserver(oldObservers, newObservers, obj::destroyObject);
//		
//		callForNewAware(oldAware, newAware, (aware) -> aware.createObject(obj));
//		callForOldAware(oldAware, newAware, (aware) -> aware.destroyObject(obj));
	}
	
	public static void callForNewObserver(Set<Player> oldObservers, Set<Player> newObservers, ObserverBasedRunnable r) {
		for (Player newObserver : newObservers) {
			if (!oldObservers.contains(newObserver))
				r.run(newObserver);
		}
	}
	
	public static void callForOldObserver(Set<Player> oldObservers, Set<Player> newObservers, ObserverBasedRunnable r) {
		for (Player oldObserver : oldObservers) {
			if (!newObservers.contains(oldObserver))
				r.run(oldObserver);
		}
	}
	
	public static void callForSameObserver(Set<Player> oldObservers, Set<Player> newObservers, ObserverBasedRunnable r) {
		for (Player newObserver : newObservers) {
			if (oldObservers.contains(newObserver))
				r.run(newObserver);
		}
	}
	
	public static void callForNewAware(Set<SWGObject> oldAware, Set<SWGObject> newAware, AwareBasedRunnable r) {
		for (SWGObject aware : newAware) {
			if (!oldAware.contains(aware))
				r.run(aware);
		}
	}
	
	public static void callForOldAware(Set<SWGObject> oldAware, Set<SWGObject> newAware, AwareBasedRunnable r) {
		for (SWGObject aware : oldAware) {
			if (!newAware.contains(aware))
				r.run(aware);
		}
	}
	
	public static void callForSameAware(Set<SWGObject> oldAware, Set<SWGObject> newAware, AwareBasedRunnable r) {
		for (SWGObject aware : newAware) {
			if (oldAware.contains(aware))
				r.run(aware);
		}
	}
	
	public interface AwareBasedRunnable {
		void run(SWGObject aware);
	}
	
	public interface ObserverBasedRunnable {
		void run(Player owner);
	}
	
}
