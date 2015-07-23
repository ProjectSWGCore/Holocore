package resources;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestQuaternion {
	
	@Test
	public void testRotation() {
		test(30, -210, 0, -1, 0, 0);
		test(30, 150, 0, 1, 0, 0);
		test(-90, -90, 0, -1, 0, 0);
		test(90, 90, 0, 1, 0, 0);
		test(90, -45, 0, Math.sin(Math.PI/8), 0, Math.cos(Math.PI/8));
//		test(180, -45, 0, Math.sin(-Math.PI/8), 0, Math.cos(-Math.PI/8));
	}
	
	private void test(double heading1, double heading2, double x, double y, double z, double w) {
		Quaternion q1 = new Quaternion(0, 0, 0, 1);
		q1.setHeading(heading1);
		Quaternion q2 = new Quaternion(0, 0, 0, 1);
		q2.setHeading(heading2);
		Quaternion ret = new Quaternion(q1);
		ret.rotateByQuaternion(q2);
		Assert.assertEquals(x, ret.getX(), 1E-7);
		Assert.assertEquals(y, ret.getY(), 1E-7);
		Assert.assertEquals(z, ret.getZ(), 1E-7);
		Assert.assertEquals(w, ret.getW(), 1E-7);
//		Assert.assertEquals((heading1+heading2+360)%360, (ret.getYaw()+360)%360, 1E-7);
	}
}
