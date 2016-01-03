package network.encryption;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestCompression {
	
	@Test
	public void testSmall() {
		byte [] data = new byte[101];
		for (int i = 0; i < data.length; i++)
			data[i] = (byte) (i % 100);
		byte [] compressed = Compression.compress(data);
		byte [] decompressed = Compression.decompress(compressed);
		Assert.assertArrayEquals(data, decompressed);
	}
	
	@Test
	public void testMedium() {
		byte [] data = new byte[256];
		for (int i = 0; i < data.length; i++)
			data[i] = (byte) (i % 100);
		byte [] compressed = Compression.compress(data);
		byte [] decompressed = Compression.decompress(compressed);
		Assert.assertArrayEquals(data, decompressed);
	}
	
	@Test
	public void testLarge() {
		byte [] data = new byte[1024];
		for (int i = 0; i < data.length; i++)
			data[i] = (byte) (i % 100);
		byte [] compressed = Compression.compress(data);
		Assert.assertTrue("Compressed should be less than actual. Compressed: "+compressed.length+" Data: "+data.length, compressed.length < data.length);
		byte [] decompressed = Compression.decompress(compressed);
		Assert.assertArrayEquals(data, decompressed);
	}
	
}
