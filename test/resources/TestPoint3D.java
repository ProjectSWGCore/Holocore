package resources;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestPoint3D {
	
	@Test
	public void testRotation() {
		Point3D p = new Point3D(0, 0, 1);
		Quaternion q = new Quaternion(0, 1, 0, 0);
		p.rotateAround(0, 0, 0, q);
		Assert.assertEquals(0, p.getX(), 1E-7);
		Assert.assertEquals(0, p.getY(), 1E-7);
		Assert.assertEquals(-1, p.getZ(), 1E-7);
		p.set(0, 0, 1);
		q.setHeading(45);
		p.rotateAround(0, 0, 0, q);
		Assert.assertEquals(Math.sqrt(2)/2, p.getX(), 1E-7);
		Assert.assertEquals(0, p.getY(), 1E-7);
		Assert.assertEquals(Math.sqrt(2)/2, p.getZ(), 1E-7);
	}
	
}
