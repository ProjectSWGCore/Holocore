package resources.objects.quadtree;

import java.util.ArrayList;
import java.util.List;

public class QuadTree<V> {
	
	private static final double DEFAULT_RANGE = 1E-5;
	
	private QuadNode headNode;
	private double minX, minY, maxX, maxY;
	
	public QuadTree(double minX, double minY, double maxX, double maxY) {
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
	
	public List <V> get(double x, double y) {
		return getWithinRange(x, y, DEFAULT_RANGE);
	}
	
	public List <V> getWithinRange(double x, double y, double range) {
		List <V> list = new ArrayList<V>();
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
		private QuadNode topLeft = null; // min, min
		private QuadNode topRight = null; // max, min
		private QuadNode bottomRight = null; // max, max
		private QuadNode bottomLeft = null; // min, max
		private double minX, minY, maxX, maxY, centerX, centerY;
		private Node obj = null;
		private int size = 0;
		
		public QuadNode(double minX, double minY, double maxX, double maxY) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.centerX = (minX+maxX) / 2;
			this.centerY = (minY+maxY) / 2;
			this.obj = null;
		}
		
		public void insert(double x, double y, V value) {
			if (Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) <= DEFAULT_RANGE || size == 0) {
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
		
		public boolean remove(double x, double y, V instance) {
			if (obj != null && Math.sqrt(Math.pow(obj.x-x,2) + Math.pow(obj.y-y,2)) <= DEFAULT_RANGE) {
				if (obj.size() == 1 && (obj.value == instance || obj.value.equals(instance))) {
					size -= 1;
					obj = null;
					return true;
				} else
					return obj.removeInstance(instance);
			}
			QuadNode quad = getQuadrant(x, y);
			if (quad != null)
				return quad.remove(x, y, instance);
			return false;
		}
		
		public int remove(double x, double y, int maxRemove) {
			if (obj != null && Math.sqrt(Math.pow(obj.x-x,2) + Math.pow(obj.y-y,2)) <= DEFAULT_RANGE)
				return removeAll(maxRemove);
			int rem = removeRecurse(x, y, maxRemove);
			size -= rem;
			return rem;
		}
		
		private int removeRecurse(double x, double y, int maxRemove) {
			QuadNode quad = getQuadrant(x, y);
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
		
		private QuadNode getQuadrant(double x, double y) {
			if (x < centerX) {
				if (y < centerY) {
					if (topLeft != null)
						return topLeft;
				} else {
					if (bottomLeft != null)
						return bottomLeft;
				}
			} else {
				if (y < centerY) {
					if (topRight != null)
						return topRight;
				} else {
					if (bottomRight != null)
						return bottomRight;
				}
			}
			return null;
		}
		
		private QuadNode expand(double x, double y) {
			if (x <= centerX) {
				if (y <= centerY) { // top-left
					if (topLeft == null)
						topLeft = new QuadNode(minX, minY, centerX, centerY);
					return topLeft;
				} else { // bottom-left
					if (bottomLeft == null)
						bottomLeft = new QuadNode(minX, centerY, centerX, maxY);
					return bottomLeft;
				}
			} else {
				if (y <= centerY) { // top-right
					if (topRight == null)
						topRight = new QuadNode(centerX, minY, maxX, centerY);
					return topRight;
				} else { // bottom-right
					if (bottomRight == null)
						bottomRight = new QuadNode(centerX, centerY, maxX, maxY);
					return bottomRight;
				}
			}
		}
		
		private void insertNode(double x, double y, V v) {
			if (obj == null)
				obj = new Node(x, y, v, null);
			else
				obj.insert(v);
		}
		
		public void getWithinRange(List <V> list, double x, double y, double range) {
			if (obj != null && Math.sqrt(Math.pow(obj.x-x,2) + Math.pow(obj.y-y,2)) <= range) {
				obj.addAll(list);
			}
			if (topLeft != null && topLeft.intersects(x, y, range))
				topLeft.getWithinRange(list, x, y, range);
			if (topRight != null && topRight.intersects(x, y, range))
				topRight.getWithinRange(list, x, y, range);
			if (bottomRight != null && bottomRight.intersects(x, y, range))
				bottomRight.getWithinRange(list, x, y, range);
			if (bottomLeft != null && bottomLeft.intersects(x, y, range))
				bottomLeft.getWithinRange(list, x, y, range);
		}
		
		public V getIgnoreCollisions(double x, double y) {
			if (obj != null)
				return obj.value;
			if (x < centerX) {
				if (y < centerY) {
					if (topLeft == null)
						return null;
					return topLeft.getIgnoreCollisions(x, y);
				} else {
					if (bottomLeft == null)
						return null;
					return bottomLeft.getIgnoreCollisions(x, y);
				}
			} else {
				if (y < centerY) {
					if (topRight == null)
						return null;
					return topRight.getIgnoreCollisions(x, y);
				} else {
					if (bottomRight == null)
						return null;
					return bottomRight.getIgnoreCollisions(x, y);
				}
			}
		}
		
		private boolean intersects(double x, double y, double range) {
			if (x < minX) {
				if (y < minY)
					return distance(x, y, minX, minY) <= range;
				else if (y > maxY)
					return distance(x, y, minX, maxY) <= range;
				return x >= minX-range;
			} else if (x > maxX) {
				if (y < minY)
					return distance(x, y, maxX, minY) <= range;
				else if (y > maxY)
					return distance(x, y, maxX, maxY) <= range;
				return x <= maxX+range;
			}
			return y >= minY-range && y <= maxY+range;
		}
		
		private double distance(double x1, double y1, double x2, double y2) {
			return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
		}
	}
	
	private class Node {
		
		private double x, y;
		private V value;
		private Node next;
		
		public Node (double x, double y, V value, Node next) {
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
		
		public int addAll(List <V> list) {
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
			return next.get(i-1);
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
	
}
