package resources.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LittleEndianDataOutputStream extends OutputStream {  // phew, what a mouthful
	
	private final DataOutputStream stream;
	
	public LittleEndianDataOutputStream(OutputStream out) {
		this.stream = new DataOutputStream(out);
	}
	
	public void write(byte [] b) throws IOException {
		stream.write(b);
	}
	
	public void write(int b) throws IOException {
		stream.writeByte(b);
	}
	
	public void writeByte(int b) throws IOException {
		stream.writeByte(b);
	}
	
	public void writeShort(int s) throws IOException {
		stream.writeShort(Short.reverseBytes((short) s));
	}
	
	public void writeInt(int i) throws IOException {
		stream.writeInt(Integer.reverseBytes(i));
	}
	
	public void writeLong(long l) throws IOException {
		stream.writeLong(Long.reverseBytes(l));
	}
	
	public void writeFloat(float f) throws IOException {
		stream.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(f)));
	}
	
	public void writeDouble(double d) throws IOException {
		stream.writeLong(Long.reverseBytes(Double.doubleToRawLongBits(d)));
	}
	
}
