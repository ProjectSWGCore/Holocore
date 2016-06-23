package resources.collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSWGFlag {
	
	@Test
	public void testFlag() {
		SWGFlag flag = new SWGFlag(3, 16);
		flag.set(1);
		flag.set(4);
		flag.set(32);
		flag.set(64);
		flag.set(96);
		int [] ints = flag.toList();
		Assert.assertEquals(4, ints.length);
		Assert.assertEquals((1<<4)+(1<<1), ints[0]);
		Assert.assertEquals(1, ints[1]);
		Assert.assertEquals(1, ints[2]);
		Assert.assertEquals(1, ints[3]);
	}
	
}
