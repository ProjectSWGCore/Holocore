package resources;

import static org.junit.Assert.assertTrue;
import java.util.Iterator;
import org.junit.Test;

public class TestSortedLinkedList {

	/**
	 * Characters are compared by numerical value and they
	 * should therefore be stored in such order within the list.
	 */
	@Test
	public void testSorting() {
		SortedLinkedList<Character> list = new SortedLinkedList<>();
		final char testchar1 = 'a';
		final char testchar2 = 'b';
		final char testchar3 = 'c';
		Iterator<Character> it;
		
		list.add(testchar2);
		list.add(testchar3);
		list.add(testchar1);
		
		it = list.listIterator();
		
		assertTrue(it.next().equals(testchar1));
		assertTrue(it.next().equals(testchar2));
		assertTrue(it.next().equals(testchar3));
	}
	
}
