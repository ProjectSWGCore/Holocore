package resources;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;

public class SortedLinkedList<E extends Comparable<E>> extends LinkedList<E> {
	
	private static final long serialVersionUID = -6776628467181994889L;
	
	public SortedLinkedList() {
		
	}
	
	@Override
	public boolean add(E e) {
		Comparable<E> element = (Comparable<E>) e;
		ListIterator<E> iter = listIterator();
		while (iter.hasNext()) {
			E item = iter.next();
			if (element.compareTo(item) <= 0) {
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
