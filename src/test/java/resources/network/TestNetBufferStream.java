package resources.network;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.network.NetBufferStream;

@RunWith(JUnit4.class)
public class TestNetBufferStream {
	
	@Test
	public void testExpansionSingle() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(5);
		}
	}
	
	@Test
	public void testExpansionBulk() {
		byte [] data = new byte[1024];
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(data);
		}
	}
	
	@Test
	public void testWriteReadString() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++)
				Assert.assertEquals(getTestString(i), stream.getAscii());
		}
	}
	
	@Test
	public void testWriteReadCompactString() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++) {
				Assert.assertEquals(getTestString(i), stream.getAscii());
				if (i == 4) {
					int rem = stream.remaining();
					stream.compact();
					Assert.assertEquals(0, stream.position());
					Assert.assertEquals(rem, stream.remaining());
				}
			}
		}
	}
	
	@Test
	public void testWriteReadInterleave() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++) {
				stream.write(generateTestString(i));
				if (i % 2 == 1) {
					Assert.assertEquals(getTestString(i-1), stream.getAscii());
					Assert.assertEquals(getTestString(i), stream.getAscii());
				}
			}
			Assert.assertEquals(0, stream.remaining());
		}
	}
	
	@Test
	public void testWriteReadCompactInterleave() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++) {
				stream.write(generateTestString(i));
				if (i % 2 == 1) {
					Assert.assertEquals(getTestString(i-1), stream.getAscii());
					Assert.assertEquals(getTestString(i), stream.getAscii());
					int rem = stream.remaining();
					stream.compact();
					Assert.assertEquals(0, stream.position());
					Assert.assertEquals(rem, stream.remaining());
				}
			}
			Assert.assertEquals(0, stream.remaining());
		}
	}
	
	@Test
	public void testRewind() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++) {
				Assert.assertEquals(getTestString(i), stream.getAscii());
				if (i == 4)
					stream.mark();
			}
			Assert.assertEquals(0, stream.remaining());
			stream.rewind();
			Assert.assertEquals(5*(2+getTestString(0).length()), stream.remaining());
			for (int i = 5; i < 10; i++)
				Assert.assertEquals(getTestString(i), stream.getAscii());
		}
	}
	
	private byte [] generateTestString(int num) {
		String test = getTestString(num);
		byte [] data = new byte[2 + test.length()];
		data[0] = (byte) test.length();
		System.arraycopy(test.getBytes(StandardCharsets.US_ASCII), 0, data, 2, data[0]);
		return data;
	}
	
	private String getTestString(int num) {
		return "Hello World " + num + "!";
	}
	
}
