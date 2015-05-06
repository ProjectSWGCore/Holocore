package resources;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.*;

public class TestSortedLinkedList {

	private static SortedLinkedList<Character> list;
	private static final char TESTCHAR1 = 'a';
	private static final char TESTCHAR2 = 'b';
	private static final char TESTCHAR3 = 'c';

	/**
	 * Characters are compared by numerical value and they
	 * should therefore be stored in such order within the list.
	 */
	@Test
	public void testSorting() {
		resetList();
		Iterator<Character> it;
		
		list.add(TESTCHAR2);
		list.add(TESTCHAR3);
		list.add(TESTCHAR1);
		
		it = list.listIterator();
		
		assertTrue(it.next().equals(TESTCHAR1));
		assertTrue(it.next().equals(TESTCHAR2));
		assertTrue(it.next().equals(TESTCHAR3));
	}
	
	private static void resetList() {
		list = new SortedLinkedList<>();
	}
	
}
