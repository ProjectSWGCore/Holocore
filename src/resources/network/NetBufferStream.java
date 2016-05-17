package resources.network;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import resources.collections.SWGList;
import resources.collections.SWGMap;
import resources.collections.SWGSet;
import resources.encodables.Encodable;
import utilities.Encoder.StringType;

public class NetBufferStream extends OutputStream {
	
	private final Object expansionMutex;
	private final Object bufferMutex;
	private NetBuffer buffer;
	private int capacity;
	private int size;
	private int mark;
	
	public NetBufferStream() {
		this(1024);
	}
	
	public NetBufferStream(int size) {
		if (size <= 0)
			throw new NegativeArraySizeException("Size cannot be less than or equal to 0!");
		this.expansionMutex = new Object();
		this.bufferMutex = new Object();
		this.buffer = NetBuffer.allocate(size);
		this.capacity = size;
		this.size = 0;
		this.mark = 0;
	}
	
	@Override
	public void close() {
		reset();
	}
	
	@Override
	public void flush() {
		
	}
	
	/**
	 * Sets the mark to the buffer's current position
	 */
	public void mark() {
		synchronized (bufferMutex) {
			mark = buffer.position();
		}
	}
	
	/**
	 * Rewinds the buffer to the previously set mark
	 */
	public void rewind() {
		synchronized (bufferMutex) {
			buffer.position(mark);
			mark = 0;
		}
	}
	
	/**
	 * Resets the buffer to the default capacity and clears all data
	 */
	public void reset() {
		synchronized (expansionMutex) {
			synchronized (bufferMutex) {
				buffer = NetBuffer.allocate(1024);
				capacity = 1024;
				size = 0;
				mark = 0;
			}
		}
	}
	
	@Override
	public void write(int b) {
		ensureCapacity(size + 1);
		synchronized (bufferMutex) {
			buffer.array()[size] = (byte) b;
			size++;
		}
	}
	
	public void write(byte [] data) {
		write(data, 0, data.length);
	}
	
	public void write(byte [] data, int offset, int length) {
		ensureCapacity(size + length);
		synchronized (bufferMutex) {
			System.arraycopy(data, offset, buffer.array(), size, length);
			size += length;
		}
	}
	
	public void write(ByteBuffer data) {
		ensureCapacity(size + data.remaining());
		synchronized (bufferMutex) {
			while (data.hasRemaining()) {
				buffer.array()[size++] = data.get();
			}
		}
	}
	
	/**
	 * Moves all data from the buffer's current position to position 0. This
	 * method also adjusts the mark to be pointing to the same data
	 */
	public void compact() {
		synchronized (bufferMutex) {
			byte [] data = buffer.array();
			for (int i = buffer.position(), j = 0; i < size; ++i, ++j) {
				data[j] = data[i];
			}
			size -= buffer.position();
			mark -= buffer.position();
			buffer.position(0);
		}
	}
	
	public int remaining() {
		synchronized (bufferMutex) {
			return size - buffer.position();
		}
	}
	
	public boolean hasRemaining() {
		return remaining() > 0;
	}
	
	public int position() {
		synchronized (bufferMutex) {
			return buffer.position();
		}
	}
	
	public void position(int position) {
		synchronized (bufferMutex) {
			buffer.position(position);
		}
	}
	
	public void seek(int relative) {
		synchronized (bufferMutex) {
			buffer.seek(relative);
		}
	}
	
	public ByteBuffer getBuffer() {
		synchronized (bufferMutex) {
			return buffer.getBuffer();
		}
	}
	
	public boolean getBoolean() {
		synchronized (bufferMutex) {
			return buffer.getBoolean();
		}
	}
	
	public String getAscii() {
		synchronized (bufferMutex) {
			return buffer.getAscii();
		}
	}
	
	public String getUnicode() {
		synchronized (bufferMutex) {
			return buffer.getUnicode();
		}
	}
	
	public String getString(StringType type) {
		synchronized (bufferMutex) {
			return buffer.getString(type);
		}
	}
	
	public byte getByte() {
		synchronized (bufferMutex) {
			return buffer.getByte();
		}
	}
	
	public short getShort() {
		synchronized (bufferMutex) {
			return buffer.getShort();
		}
	}
	
	public int getInt() {
		synchronized (bufferMutex) {
			return buffer.getInt();
		}
	}
	
	public float getFloat() {
		synchronized (bufferMutex) {
			return buffer.getFloat();
		}
	}
	
	public long getLong() {
		synchronized (bufferMutex) {
			return buffer.getLong();
		}
	}
	
	public short getNetShort() {
		synchronized (bufferMutex) {
			return buffer.getNetShort();
		}
	}
	
	public int getNetInt() {
		synchronized (bufferMutex) {
			return buffer.getNetInt();
		}
	}
	
	public long getNetLong() {
		synchronized (bufferMutex) {
			return buffer.getNetLong();
		}
	}
	
	public byte[] getArray() {
		synchronized (bufferMutex) {
			return buffer.getArray();
		}
	}
	
	public byte[] getArray(int size) {
		synchronized (bufferMutex) {
			return buffer.getArray(size);
		}
	}
	
	public <T> Object getGeneric(Class<T> type) {
		synchronized (bufferMutex) {
			return buffer.getGeneric(type);
		}
	}
	
	public <T extends Encodable> T getEncodable(Class<T> type) {
		synchronized (bufferMutex) {
			return buffer.getEncodable(type);
		}
	}
	
	public SWGSet<String> getSwgSet(int num, int var, StringType type) {
		synchronized (bufferMutex) {
			return buffer.getSwgSet(num, var, type);
		}
	}
	
	public <T> SWGSet<T> getSwgSet(int num, int var, Class<T> type) {
		synchronized (bufferMutex) {
			return buffer.getSwgSet(num, var, type);
		}
	}
	
	public SWGList<String> getSwgList(int num, int var, StringType type) {
		synchronized (bufferMutex) {
			return buffer.getSwgList(num, var, type);
		}
	}
	
	public <T> SWGList<T> getSwgList(int num, int var, Class<T> type) {
		synchronized (bufferMutex) {
			return buffer.getSwgList(num, var, type);
		}
	}
	
	public SWGMap<String, String> getSwgMap(int num, int var, StringType strType) {
		synchronized (bufferMutex) {
			return buffer.getSwgMap(num, var, strType);
		}
	}
	
	public <V> SWGMap<String, V> getSwgMap(int num, int var, StringType key, Class<V> val) {
		synchronized (bufferMutex) {
			return buffer.getSwgMap(num, var, key, val);
		}
	}
	
	public <K, V> SWGMap<K, V> getSwgMap(int num, int var, Class<K> key, Class<V> val) {
		synchronized (bufferMutex) {
			return buffer.getSwgMap(num, var, key, val);
		}
	}
	
	public byte [] array() {
		synchronized (bufferMutex) {
			return buffer.array();
		}
	}
	
	public int size() {
		return size;
	}
	
	public int capacity() {
		return capacity;
	}
	
	private void ensureCapacity(int size) {
		if (size <= capacity)
			return;
		synchronized (expansionMutex) {
			while (size > capacity)
				capacity <<= 2;
			synchronized (bufferMutex) {
				NetBuffer buf = NetBuffer.allocate(capacity);
				System.arraycopy(buffer.array(), 0, buf.array(), 0, this.size);
				buf.position(buffer.position());
				this.buffer = buf;
			}
		}
	}
	
}
