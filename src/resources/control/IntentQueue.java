package resources.control;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Queue;

class IntentQueue implements Queue<Intent> {
	
	private final Node head;
	private int size;
	private int modificationCount;
	
	public IntentQueue() {
		head = new Node(null, null, null); // Left = Forward, Right = Reverse
		head.left = head;
		head.right = head;
		modificationCount = 0;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	
	@Override
	public boolean contains(Object o) {
		Node n = head;
		while (n.left != head) {
			if (n.left.intent == o)
				return true;
		}
		return false;
	}
	
	@Override
	public Iterator iterator() {
		return new Iterator();
	}

	@Override
	public Object [] toArray() {
		return null;
	}
	
	@Override
	public <T> T [] toArray(T [] a) {
		return null;
	}
	
	@Override
	public boolean remove(Object o) {
		modificationCount++;
		Node n = head;
		while (n.left != head) {
			if (n.left.intent == o) {
				n.left = n.left.left;
				n.left.right = n;
			}
		}
		return false;
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends Intent> c) {
		boolean added = false;
		for (Intent i : c)
			added = add(i) || added;
		return added;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c)
			changed = remove(o) || changed;
		return changed;
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		Node n = head;
		while (n.left != head) {
			if (!c.contains(n.left.intent)) {
				n.left.left.right = n;
				n.left = n.left.left;
			}
		}
		return changed;
	}
	
	@Override
	public void clear() {
		modificationCount++;
		size = 0;
		head.left = head;
		head.right = head;
	}
	
	@Override
	public boolean add(Intent e) {
		head.right.left = new Node(e, head, head.right);
		head.right = head.right.left;
		modificationCount++;
		size++;
		return true;
	}
	
	@Override
	public boolean offer(Intent e) {
		return add(e);
	}
	
	@Override
	public Intent remove() {
		if (isEmpty())
			throw new NoSuchElementException("Queue is empty!");
		modificationCount++;
		Intent i = head.left.intent;
		head.left = head.left.left;
		head.left.right = head;
		size--;
		return i;
	}
	
	@Override
	public Intent poll() {
		if (isEmpty())
			return null;
		modificationCount++;
		Intent i = head.left.intent;
		head.left = head.left.left;
		head.left.right = head;
		size--;
		return i;
	}
	
	@Override
	public Intent element() {
		if (isEmpty())
			throw new NoSuchElementException("Queue is empty!");
		return head.left.intent;
	}
	
	@Override
	public Intent peek() {
		if (isEmpty())
			return null;
		return head.left.intent;
	}
	
	private class Iterator implements java.util.Iterator<Intent> {
		
		private final int modificationCount;
		private Node currentNode;
		
		public Iterator() {
			this.modificationCount = IntentQueue.this.modificationCount;
			this.currentNode = IntentQueue.this.head;
		}
		
		@Override
		public boolean hasNext() {
			if (this.modificationCount != IntentQueue.this.modificationCount)
				throw new ConcurrentModificationException();
			return currentNode.left != IntentQueue.this.head;
		}
		
		@Override
		public Intent next() {
			if (!hasNext())
				throw new NoSuchElementException("Iterator has reached the end of the queue!");
			Intent i = currentNode.left.intent;
			currentNode = currentNode.left;
			return i;
		}
		
	}
	
	private static class Node {
		
		public final Intent intent;
		public Node left;
		public Node right;
		
		public Node(Intent intent, Node left, Node right) {
			this.intent = intent;
			this.left = left;
			this.right = right;
		}
		
	}
	
}
