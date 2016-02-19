package network.encryption;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

public class Compression {
	
	private static final LZ4Compressor COMPRESSOR = LZ4Factory.safeInstance().highCompressor();
	private static final LZ4SafeDecompressor DECOMPRESSOR = LZ4Factory.safeInstance().safeDecompressor();
	
	public static int getMaxCompressedLength(int len) {
		return COMPRESSOR.maxCompressedLength(len);
	}
	
	public static int compress(byte [] data, byte [] buffer) {
		return COMPRESSOR.compress(data, buffer);
	}
	
	public static byte [] decompress(byte [] data) {
		return decompress(data, data.length * 10);
	}
	
	public static byte [] decompress(byte [] data, int bufferSize) {
		byte [] restored = new byte[bufferSize];
		int length = DECOMPRESSOR.decompress(data, restored);
		if (length == bufferSize)
			return restored;
		byte [] ret = new byte[length];
		System.arraycopy(restored, 0, ret, 0, length);
		return ret;
	}
	
}
