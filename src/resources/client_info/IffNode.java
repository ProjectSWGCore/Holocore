package resources.client_info;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Waverunner on 6/7/2015
 */
public class IffNode {

	private String tag;
	private boolean isForm;
	private List<IffNode> children;
	private byte[] chunkData;

	public IffNode(String tag, boolean isForm) {
		this.tag = tag;
		this.isForm = isForm;
	}

	public void addChild(IffNode child) {
		if (children == null)
			children = new ArrayList<>();
		children.add(child);
	}

	public void setChunkData(ByteBuffer chunkData) {
		this.chunkData = chunkData.array();
	}

	public byte[] getBytes() {
		return isForm ? createForm() : createChunk();
	}

	private byte[] createForm() {
		List<byte[]> childrenData = new ArrayList<>();
		int size = 0;
		for (IffNode child : children) {
			byte[] subData = child.getBytes();
			size += subData.length;
			childrenData.add(subData);
		}
		ByteBuffer bb = ByteBuffer.allocate(size + 12).order(ByteOrder.LITTLE_ENDIAN);
		bb.put("FORM".getBytes(Charset.forName("US-ASCII")));
		bb.order(ByteOrder.BIG_ENDIAN).putInt(size + 4);
		bb.put(tag.getBytes(Charset.forName("US-ASCII")));
		for (byte[] bytes : childrenData) {
			bb.put(bytes);
		}
		return bb.array();
	}

	private byte[] createChunk() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 + chunkData.length).order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(tag.getBytes(Charset.forName("US-ASCII")));
		byteBuffer.order(ByteOrder.BIG_ENDIAN).putInt(chunkData.length);
		byteBuffer.put(chunkData);
		return byteBuffer.array();
	}
}