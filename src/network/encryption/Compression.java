package network.encryption;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

public class Compression {
	
	private static final LZ4Compressor COMPRESSOR = LZ4Factory.safeInstance().highCompressor();
	private static final LZ4SafeDecompressor DECOMPRESSOR = LZ4Factory.safeInstance().safeDecompressor();
	
	public static byte [] compress(byte [] data) {
		int maxCompressedLength = COMPRESSOR.maxCompressedLength(data.length);
		byte[] compressed = new byte[maxCompressedLength];
		int length = COMPRESSOR.compress(data, compressed);
		byte [] ret = new byte[length];
		System.arraycopy(compressed, 0, ret, 0, length);
		return ret;
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
