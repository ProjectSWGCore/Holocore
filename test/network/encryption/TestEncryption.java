package network.encryption;

import java.util.Random;

import network.packets.soe.Acknowledge;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestEncryption {
	
	@Test
	public void testEncryptionSmall() {
		Random r = new Random();
		int crc = r.nextInt();
		byte [] testInput = new byte[10];
		r.nextBytes(testInput);
		testInput[0] = 0;
		testInput[1] = 9;
		byte [] encoded = Encryption.encode(testInput, crc);
		byte [] decoded = Encryption.decode(encoded, crc);
		Assert.assertArrayEquals(testInput, decoded);
	}
	
	@Test
	public void testEncryptionLarge() {
		Random r = new Random();
		int crc = r.nextInt();
		byte [] testInput = new byte[256];
		r.nextBytes(testInput);
		testInput[0] = 0;
		testInput[1] = 9;
		byte [] encoded = Encryption.encode(testInput, crc);
		byte [] decoded = Encryption.decode(encoded, crc);
		Assert.assertArrayEquals(testInput, decoded);
	}
	
//	@Test
//	public void testEncryptionCorrectLarge() {
//		Random r = new Random();
//		int crc = r.nextInt();
//		byte [] testInput = new byte[256];
//		r.nextBytes(testInput);
//		testInput[0] = 0;
//		testInput[1] = 9;
//		MessageCRC cr = new MessageCRC();
//		MessageEncryption e = new MessageEncryption();
//		MessageCompression co = new MessageCompression();
//		byte [] encoded = Encryption.encode(testInput, crc);
//		byte [] expected = cr.append(e.encrypt(co.compress(testInput), crc), crc);
//		Assert.assertArrayEquals(expected, encoded);
//	}
	
//	@Test
//	public void testEncoding() {
//		Random r = new Random();
//		int crc = r.nextInt();
//		MessageCRC cr = new MessageCRC();
//		MessageEncryption e = new MessageEncryption();
//		MessageCompression co = new MessageCompression();
//		for (int size = 6; size < 496; size++) {
//			byte [] testInput = new byte[size];
//			r.nextBytes(testInput);
//			testInput[0] = 0;
////			testInput[1] = 9;
//			byte [] encoded = Encryption.encode(testInput, crc);
//			byte [] expected = cr.append(e.encrypt(co.compress(testInput), crc), crc);
//			Assert.assertArrayEquals(expected, encoded);
//		}
//	}
	
	@Test
	public void testAcknowledge() {
		Random r = new Random();
		int crc = r.nextInt();
		System.out.println(Integer.toHexString(crc));
		Acknowledge a = new Acknowledge((short) 5);
		byte [] data = a.encode().array();
		byte [] encoded = Encryption.encode(data, crc);
		byte [] decoded = Encryption.decode(encoded, crc);
		Assert.assertArrayEquals(data, decoded);
	}
	
}
