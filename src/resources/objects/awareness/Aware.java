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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerState;

class Aware {
	
	private final SWGObject object;
	private final List<Aware> awareness;
	private final AtomicReference<Aware> parent;
	
	public Aware(SWGObject obj) {
		this.object = Objects.requireNonNull(obj, "Object cannot be null!");
		this.awareness = new ArrayList<>();
		this.parent = new AtomicReference<>(null);
	}
	
	public SWGObject getObject() {
		return object;
	}
	
	public void setParent(Aware parent) {
		this.parent.set(parent);
	}
	
	public boolean add(Aware a) {
		boolean success = internalAdd(a);
		return a.internalAdd(this) && success;
	}
	
	public boolean remove(Aware a) {
		boolean success = internalRemove(a);
		return a.internalRemove(this) && success;
	}
	
	public boolean contains(SWGObject obj) {
		synchronized (awareness) {
			for (Aware a : awareness) {
				if (a.getObject().equals(obj))
					return true;
			}
			return false;
		}
	}
	
	public void clear() {
		List<Aware> aware;
		synchronized (awareness) {
			aware = new ArrayList<>(awareness);
		}
		for (Aware a : aware)
			remove(a);
	}
	
	public Set<SWGObject> getAware() {
		Set<SWGObject> aware = new HashSet<>(awareness.size());
		synchronized (awareness) {
			for (Aware a : awareness) {
				aware.add(a.getObject());
			}
		}
		Aware parent = getParent();
		if (parent != null)
			aware.addAll(parent.getAware());
		return aware;
	}
	
	public Set<Player> getObservers() {
		Player owner = object.getOwner();
		if (getParent() == null)
			return getObservers(owner, getObject(), true);
		return getSuperParent().getObservers(owner, getObject(), true);
	}
	
	private boolean internalAdd(Aware binding) {
		synchronized (awareness) {
			return awareness.add(binding);
		}
	}
	
	private boolean internalRemove(Aware binding) {
		synchronized (awareness) {
			return awareness.remove(binding);
		}
	}
	
	private Aware getParent() {
		return parent.get();
	}
	
	private Aware getSuperParent() {
		Aware tmpParent = getParent();
		Aware ret = tmpParent;
		if (tmpParent == null)
			return ret;
		while ((tmpParent = tmpParent.getParent()) != null) {
			ret = tmpParent;
		}
		return ret;
	}
	
	private Set<Player> getObservers(Player owner, SWGObject original, boolean useAware) {
		Set<Player> observers = new HashSet<>();
		addObserversToSet(getObject().getContainedObjects(), observers, owner, original);
		addObserversToSet(getObject().getSlottedObjects(), observers, owner, original);
		if (useAware) {
			addObserversToSet(getAware(), observers, owner, original);
		}
		return observers;
	}
	
	public static void addObserversToSet(Collection<SWGObject> nearby, Collection<Player> observers, Player owner, SWGObject original) {
		for (SWGObject aware : nearby) {
			if (!aware.isVisible(original))
				continue;
			if (isObserver(aware, owner, original))
				observers.add(aware.getOwnerShallow());
			else
				addObserversToSet(aware.getContainedObjects(), observers, owner, original);
		}
	}
	
	public static boolean isObserver(SWGObject aware, Player owner, SWGObject original) {
		if (!(aware instanceof CreatureObject))
			return false;
		Player awareOwner = aware.getOwner();
		if (awareOwner == null || awareOwner.equals(owner))
			return false;
		if (awareOwner.getPlayerState() != PlayerState.ZONED_IN)
			return false;
		return ((CreatureObject) aware).isLoggedInPlayer();
	}
	
}
