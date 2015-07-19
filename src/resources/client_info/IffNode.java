/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 * <p>
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on July 7th, 2011 after SOE announced the
 * official shutdown of Star Wars Galaxies. Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing it on the final publish of the game prior
 * to end-game events.
 * <p>
 * This file is part of Holocore.
 * <p>
 * --------------------------------------------------------------------------------
 * <p>
 * Holocore is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Holocore is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License along with Holocore.  If not, see
 * <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.client_info;

import utilities.ByteUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Waverunner on 6/7/2015
 */
public class IffNode {

	private String tag;
	private boolean isForm;
	private boolean read;
	private IffNode parent;
	private List<IffNode> children;
	private byte[] chunkData;
	private ByteBuffer bb;
	private Iterator<IffNode> childrenItr;

	public IffNode() {
	}

	public IffNode(String tag, boolean isForm) {
		this.tag = tag;
		this.isForm = isForm;
	}

	public void addChild(IffNode child) {
		if (children == null)
			children = new LinkedList<>();
		children.add(child);
	}

	public byte readByte() {
		initBuffer();
		return bb.get();
	}

	public short readShort() {
		initBuffer();
		return bb.order(ByteOrder.LITTLE_ENDIAN).getShort();
	}

	public int readInt() {
		initBuffer();
		return bb.order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public float readFloat() {
		initBuffer();
		return bb.getFloat();
	}

	public long readLong() {
		initBuffer();
		return bb.order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	public int readUInt() {
		initBuffer();
		return bb.order(ByteOrder.BIG_ENDIAN).getInt();
	}

	public short readUShort() {
		initBuffer();
		return bb.order(ByteOrder.BIG_ENDIAN).getShort();
	}

	public long readULong() {
		initBuffer();
		return bb.order(ByteOrder.BIG_ENDIAN).getLong();
	}

	public boolean readBoolean() {
		return readByte() == 1;
	}

	public void skip(int offset) {
		initBuffer();
		bb.position(bb.position() + offset);
	}

	public String readString() {
		initBuffer();
		String string = ByteUtilities.nextString(bb);
		if (!string.isEmpty())
			bb.get();
		return string;
	}

	public void readChunk(ChunkReader reader) {
		initBuffer();
		while(bb.hasRemaining()) {
			reader.handleReadChunk(this);
		}
	}

	public byte[] getBytes() {
		return isForm ? createForm() : createChunk();
	}

	public String getTag() {
		return tag;
	}

	public int getVersionFromTag() {
		return Integer.parseInt(tag);
	}

	public List<IffNode> getChildren() {
		return children;
	}

	public boolean isForm() {
		return isForm;
	}

	public boolean hasBeenRead() {
		return read;
	}

	public IffNode getNextUnreadChunk() {
		while(childrenItr.hasNext()) {
			IffNode child = childrenItr.next();
			if (!child.isForm && !child.hasBeenRead())
				return child;
		}
		return null;
	}

	public IffNode getNextUnreadForm() {
		while(childrenItr.hasNext()) {
			IffNode child = childrenItr.next();
			if (child.isForm && !child.hasBeenRead())
				return child;
		}
		return null;
	}

	public void setHasBeenRead(boolean read) {
		this.read = read;
	}

	public byte[] getChunkData() {
		if (chunkData == null)
			System.err.println("Null chunk data for " + tag + " Form=" + isForm);
		return chunkData;
	}

	public void setChunkData(ByteBuffer chunkData) {
		this.chunkData = chunkData.array();
	}

	public IffNode getParent() {
		return parent;
	}

	public int populateFromBuffer(ByteBuffer bb) {
		String nodeTag = getTag(bb);
		if (!nodeTag.equals("FORM")) {
			tag = nodeTag;
			return 4 + readChunk(bb);
		} else return 4 + readForm(bb);
	}

	private void initBuffer() {
		if (bb == null) bb = ByteBuffer.wrap(getChunkData());
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

	private int readForm(ByteBuffer bb) {
		isForm = true;
		int size = bb.getInt(); // Size includes the "FORM" length (4)
		tag = getTag(bb);
		for (int read = 4; read < size; read++) {
			IffNode child = new IffNode();
			child.parent = this;

			read += child.populateFromBuffer(bb);
			addChild(child);
		}
		if (children != null)
			childrenItr = children.listIterator();

		return 4 + size;
	}

	private byte[] createChunk() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 + chunkData.length).order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(tag.getBytes(Charset.forName("US-ASCII")));
		byteBuffer.order(ByteOrder.BIG_ENDIAN).putInt(chunkData.length);
		byteBuffer.put(chunkData);
		return byteBuffer.array();
	}

	private int readChunk(ByteBuffer bb) {
		isForm = false;
		int chunkSize = bb.getInt();
		chunkData = new byte[chunkSize];
		bb.get(chunkData);

		return 4 + chunkData.length;
	}

	private String getTag(ByteBuffer bb) {
		byte[] tagBytes = new byte[4];
		bb.get(tagBytes);
		return new String(tagBytes);
	}

	@Override
	public String toString() {
		return "IffNode[tag='" + tag + '\'' + ", read=" + read + ", parent=" + parent + ", isForm=" + isForm + "]";
	}
}