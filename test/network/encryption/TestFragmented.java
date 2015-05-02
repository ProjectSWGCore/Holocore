package network.encryption;

import java.nio.ByteBuffer;
import java.util.Random;

import network.packets.soe.Fragmented;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestFragmented {
	
	@Test
	public void testFragmented() {
		Random r = new Random();
		byte [] randomData = new byte[1024];
		r.nextBytes(randomData);
		test(randomData);
	}
	
	@Test
	public void testFragmentedSmall() {
		Random r = new Random();
		byte [] randomData = new byte[100];
		r.nextBytes(randomData);
		test(randomData);
	}
	
	@Test
	public void testFragmentedEverySize() {
		for (int i = 0; i < 496*4; i++) {
			byte [] junkData = new byte[i];
			Fragmented main = new Fragmented();
			main.setPacket(ByteBuffer.wrap(junkData));
			try {
				main.encode(5);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Failed to encode at fragmented size " + i);
			}
		}
	}
	
	private void test(byte [] data) {
		FragmentedChannelA fragCore2 = new FragmentedChannelA();
		FragmentedChannelA [] fragsCore2 = fragCore2.create(data);
		
		Fragmented fragCore3 = new Fragmented();
		fragCore3.setPacket(ByteBuffer.wrap(data));
		Fragmented [] fragsCore3 = fragCore3.encode(5);
		
		Assert.assertEquals(fragsCore2.length, fragsCore3.length);
		for (int i = 0; i < fragsCore2.length; i++) {
			fragsCore2[i].setSequence((short) (i + 5));
			byte [] c2 = fragsCore2[i].serialize().array();
			byte [] c3 = fragsCore3[i].encode().array();
			Assert.assertArrayEquals(c2, c3);
		}
	}
	
}
