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
package resources.objects.quadtree;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class QuadTree<V> {
	
	private static final double	DEFAULT_RANGE	= 1E-5;
	
	private final QuadNode	headNode;
	private final double	minX, minY, maxX, maxY;
	private final int		quadTreeSize;
	
	public QuadTree(int size, double minX, double minY, double maxX, double maxY) {
		this.quadTreeSize = size;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		headNode = new QuadNode(minX, minY, maxX, maxY);
	}
	
	public void put(double x, double y, V value) {
		if (value == null || Double.isNaN(x) || Double.isNaN(y))
			return;
		if (x < minX || y < minY || x > maxX || y > maxY)
			return;
		headNode.insert(x, y, value);
	}
	
	public boolean contains(double x, double y, V obj) {
		return containsWithinRange(x, y, obj, DEFAULT_RANGE);
	}
	
	public boolean containsWithinRange(double x, double y, V obj, double range) {
		if (Double.isNaN(x) || Double.isNaN(y))
			return false;
		if (x < minX || y < minY || x > maxX || y > maxY)
			return false;
		return headNode.containsWithinRange(x, y, obj, range);
	}
	
	public List<V> get(double x, double y) {
		return getWithinRange(x, y, DEFAULT_RANGE);
	}
	
	public List<V> getWithinRange(double x, double y, double range) {
		List<V> list = new ArrayList<>();
		if (Double.isNaN(x) || Double.isNaN(y))
			return list;
		if (x < minX || y < minY || x > maxX || y > maxY)
			return list;
		headNode.getWithinRange(list, x, y, range);
		return list;
	}
	
	public boolean remove(double x, double y, V instance) {
		if (instance == null || Double.isNaN(x) || Double.isNaN(y))
			return false;
		if (x < minX || y < minY || x > maxX || y > maxY)
			return false;
		return headNode.remove(x, y, instance);
	}
	
	public int remove(double x, double y) {
		if (Double.isNaN(x) || Double.isNaN(y))
			return 0;
		if (x < minX || y < minY || x > maxX || y > maxY)
			return 0;
		return remove(x, y, -1);
	}
	
	public int remove(double x, double y, int maxRemove) {
		return headNode.remove(x, y, maxRemove);
	}
	
	public V getIgnoreCollisions(double x, double y) {
		return headNode.getIgnoreCollisions(x, y);
	}
	
	private class QuadNode {
		private final QuadSubNodes<QuadNode> subnodes;
		private final double minX, minY, maxX, maxY, centerX, centerY;
		private final double cellWidth, cellHeight;
		private Node obj;
		private int size = 0;
		
		public QuadNode(double minX, double minY, double maxX, double maxY) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.centerX = (minX + maxX) / 2;
			this.centerY = (minY + maxY) / 2;
			this.obj = null;
			this.subnodes = new QuadSubNodes<>(quadTreeSize, minX, minY, maxX, maxY);
			this.cellWidth = (maxX - minX) / quadTreeSize;
			this.cellHeight = (maxY - minY) / quadTreeSize;
		}
		
		public void insert(double x, double y, V value) {
			if (size == 0 || square(x - centerX) + square(y - centerY) <= DEFAULT_RANGE) {
				insertNode(x, y, value);
			} else {
				if (size == 1 && obj != null) {
					V v = obj.value;
					expand(obj.x, obj.y).insert(obj.x, obj.y, v);
					obj = null;
				}
				expand(x, y).insert(x, y, value);
			}
			size++;
		}
		
		private double square(double x) {
			return x * x;
		}
		
		public boolean remove(double x, double y, V instance) {
			if (obj != null && square(obj.x - x) + square(obj.y - y) <= DEFAULT_RANGE) {
				if (obj.size() == 1 && (obj.value == instance || obj.value.equals(instance))) {
					size -= 1;
					obj = null;
					return true;
				} else
					return obj.removeInstance(instance);
			}
			QuadNode quad = subnodes.get(x, y);
			return quad != null && quad.remove(x, y, instance);
		}
		
		public int remove(double x, double y, int maxRemove) {
			if (obj != null && square(obj.x - x) + square(obj.y - y) <= DEFAULT_RANGE)
				return removeAll(maxRemove);
			int rem = removeRecurse(x, y, maxRemove);
			size -= rem;
			return rem;
		}
		
		private int removeRecurse(double x, double y, int maxRemove) {
			QuadNode quad = subnodes.get(x, y);
			if (quad != null)
				return quad.remove(x, y, maxRemove);
			return 0;
		}
		
		private int removeAll(int maxRemove) {
			int objSize = obj.size();
			if (maxRemove < 0 || maxRemove >= objSize) {
				obj = null;
				size -= objSize;
				return objSize;
			}
			obj = obj.get(maxRemove);
			size -= maxRemove;
			return maxRemove;
		}
		
		private QuadNode expand(double x, double y) {
			int indX = getIndex(x, minX, cellWidth);
			int indY = getIndex(y, minY, cellHeight);
			if (indX >= subnodes.length())
				indX--;
			if (indY >= subnodes.length())
				indY--;
			QuadNode node = subnodes.getSubnode(indX, indY);
			if (node == null) {
				double mnX = minX + indX * cellWidth;
				double mnY = minY + indY * cellHeight;
				node = new QuadNode(mnX, mnY, mnX + cellWidth, mnY + cellHeight);
				subnodes.setSubnode(indX, indY, node);
			}
			return node;
		}
		
		private void insertNode(double x, double y, V v) {
			if (obj == null)
				obj = new Node(x, y, v, null);
			else
				obj.insert(v);
		}
		
		public boolean containsWithinRange(double x, double y, V v, double range) {
			if (obj != null && square(obj.x - x) + square(obj.y - y) <= square(range)) {
				return obj.contains(v);
			}
			int mnX = (x - range <= minX) ? 0 : getIndex(x - range, minX, cellWidth);
			int mnY = (y - range <= minY) ? 0 : getIndex(y - range, minY, cellHeight);
			int mxX = (x + range >= maxX) ? subnodes.length() - 1 : getIndex(x + range, minX, cellWidth);
			int mxY = (y + range >= maxY) ? subnodes.length() - 1 : getIndex(y + range, minY, cellHeight);
			for (int xInd = mnX; xInd <= mxX; xInd++) {
				for (int yInd = mnY-1; yInd <= mxY; yInd++) {
					if (yInd < 0)
						continue;
					QuadNode node = subnodes.getSubnode(xInd, yInd);
					if (node != null && node.containsWithinRange(x, y, v, range))
						return true;
				}
			}
			return false;
		}
		
		public void getWithinRange(List<V> list, double x, double y, double range) {
			if (obj != null && square(obj.x - x) + square(obj.y - y) <= square(range)) {
				obj.addAll(list);
				if (size == obj.size())
					return;
			}
			int mnX = (x - range <= minX) ? 0 : getIndex(x - range, minX, cellWidth);
			int mnY = (y - range <= minY) ? 0 : getIndex(y - range, minY, cellHeight);
			int mxX = (x + range >= maxX) ? subnodes.length() - 1 : getIndex(x + range, minX, cellWidth);
			int mxY = (y + range >= maxY) ? subnodes.length() - 1 : getIndex(y + range, minY, cellHeight);
			for (int xInd = mnX; xInd <= mxX; xInd++) {
				for (int yInd = mnY-1; yInd <= mxY; yInd++) {
					if (yInd < 0)
						continue;
					QuadNode node = subnodes.getSubnode(xInd, yInd);
					if (node != null)
						node.getWithinRange(list, x, y, range);
				}
			}
		}
		
		public V getIgnoreCollisions(double x, double y) {
			if (obj != null)
				return obj.value;
			QuadNode node = subnodes.get(x, y);
			if (node == null)
				return null;
			return node.getIgnoreCollisions(x, y);
		}
		
	}
	
	private class Node {
		
		private double	x, y;
		private V		value;
		private Node	next;
		
		public Node(double x, double y, V value, Node next) {
			this.x = x;
			this.y = y;
			this.value = value;
			this.next = next;
		}
		
		public void insert(V value) {
			if (next == null)
				next = new Node(x, y, value, null);
			else
				next.insert(value);
		}
		
		public boolean contains(V v) {
			if (value.equals(v))
				return true;
			if (next != null)
				return next.contains(v);
			return false;
		}
		
		public int addAll(List<V> list) {
			list.add(value);
			if (next != null)
				return 1 + next.addAll(list);
			return 1;
		}
		
		public Node get(int i) {
			if (i == 0)
				return this;
			if (next == null)
				return null;
			return next.get(i - 1);
		}
		
		public int size() {
			if (next == null)
				return 1;
			return 1 + next.size();
		}
		
		public boolean removeInstance(V instance) {
			if (next == null)
				return false;
			if (next.value == instance || next.value.equals(instance)) {
				next = next.next; // easy peesy
				if (next != null)
					return next.removeInstance(instance);
				return true;
			}
			return next.removeInstance(instance);
		}
		
	}
	
	private static class QuadSubNodes<T> implements Iterable<T> {
		private final T [][] subnodes;
		private final double minX, minY;
		private final double cellWidth, cellHeight;
		
		@SuppressWarnings("unchecked")
		public QuadSubNodes(int size, double minX, double minY, double maxX, double maxY) {
			this.minX = minX;
			this.minY = minY;
			this.cellWidth = (maxX - minX) / size;
			this.cellHeight = (maxY - minY) / size;
			this.subnodes = (T [][]) new Object[size][size];
		}
		
		public T get(double x, double y) {
			int indX = getIndex(x, minX, cellWidth);
			int indY = getIndex(y, minY, cellHeight);
			if (indX >= subnodes.length)
				indX--;
			if (indY >= subnodes.length)
				indY--;
			return subnodes[indX][indY];
		}
		
		public T getSubnode(int x, int y) {
			return subnodes[x][y];
		}
		
		public void setSubnode(int x, int y, T node) {
			subnodes[x][y] = node;
		}
		
		public int length() {
			return subnodes.length;
		}
		
		@Override
		public Iterator iterator() {
			return new Iterator();
		}
		
		public class Iterator implements java.util.Iterator<T> {
			
			private int	x = 0;
			private int	y = 0;
			
			@Override
			public boolean hasNext() {
				return x < subnodes.length && y < subnodes.length;
			}
			
			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException("Reached the end of the iterator!");
				T node = subnodes[x][y];
				if (y + 1 < subnodes.length)
					y++;
				else {
					x++;
					y = 0;
				}
				return node;
			}
			
			@Override
			public void remove() {
				if (y == 0) {
					subnodes[x - 1][subnodes.length - 1] = null;
				} else {
					subnodes[x][y - 1] = null;
				}
			}
			
		}
	}
	
	private static int getIndex(double pos, double min, double max) {
		return (int) ((pos - min) / max);
	}
	
}
