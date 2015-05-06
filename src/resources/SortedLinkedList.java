/***********************************************************************************
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
package resources;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;

public class SortedLinkedList<E extends Comparable<E>> extends LinkedList<E> {
	
	private static final long serialVersionUID = -6776628467181994889L;
			
	@Override
	public boolean add(E e) {
		ListIterator<E> iter = super.listIterator();
		
		while (iter.hasNext()) {
			if (e.compareTo(iter.next()) <= 0) {
				iter.previous();
				iter.add(e);
				return true;
			}
		}
		super.addLast(e);
		return true;
	}
	
	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException("The sorted nature of the list prohibits this action.");
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E item : c) {
			add(item);
		}
		
		return true;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException("The sorted nature of the list prohibits this action.");
	}
	
	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException("The sorted nature of the list prohibits this action.");
	}
	
}
