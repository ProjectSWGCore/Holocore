package resources.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestLittleEndianDataOutputStream {
	
	@Test
	public void test() throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(27).order(ByteOrder.LITTLE_ENDIAN);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LittleEndianDataOutputStream le = new LittleEndianDataOutputStream(baos);
		bb.put((byte) 15);			// 0
		bb.putShort((short) 1024);	// 1
		bb.putInt(1024);			// 3
		bb.putLong(1024);			// 7
		bb.putFloat(1.25f);			// 11
		bb.putDouble(1.75);			// 15
		le.write(15);
		le.writeShort(1024);
		le.writeInt(1024);
		le.writeLong(1024);
		le.writeFloat(1.25f);
		le.writeDouble(1.75);
		Assert.assertArrayEquals(bb.array(), baos.toByteArray());
		le.close();
	}
	
}
