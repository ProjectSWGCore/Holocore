package com.projectswg.holocore.resources.support.data.collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSWGBitSet {
	
	@Test
	public void testFlag() {
		SWGBitSet flag = new SWGBitSet(3, 16);
		flag.set(1);
		flag.set(4); // 1 byte
		flag.set(8); // 2 bytes
		flag.set(16); // 3 bytes
		flag.set(32); // 5 bytes
		byte [] actual = flag.encode();
		byte [] expected = new byte[] {
			5, 0, 0, 0,
			33, 0, 0, 0,
			(1<<4)+(1<<1), 1, 1, 0, 1
		};
		Assert.assertArrayEquals(expected, actual);
	}
	
}
