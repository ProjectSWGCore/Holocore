/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.client_info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Created by Waverunner on 6/4/2015
 */
public class SWGFile {

	private String type;
	private IffNode master;
	private IffNode currentForm;
	private String fileName;

	public SWGFile() {}

	public SWGFile(String fileName, String type) {
		this(type);
		this.fileName = fileName;
	}

	public SWGFile(String type) {
		this.type = type;
		this.master = new IffNode(type, true);
		this.currentForm = master;
	}
	
	public void printTree() {
		printTree(master, 0);
	}
	
	private void printTree(IffNode node, int depth) {
		for (int i = 0; i < depth; i++)
			System.out.print("\t");
		System.out.println(node.getTag()+":"+node.isForm());
		for (IffNode child : node.getChildren())
			printTree(child, depth+1);
	}

	public void save(File file) throws IOException {
		try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
			outputStream.write(getData());
		}
	}

	public void read(File file) throws IOException {
		FileChannel channel = FileChannel.open(file.toPath());
		MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		int size = (int) channel.size();
		channel.close();

		master = new IffNode("", true);
		currentForm = master;

		if (!isValidIff(bb, size)) {
			System.err.println("Tried to open a file not in a valid Interchangeable File Format: " + file.getAbsolutePath());
			return;
		}

		if (size != master.populateFromBuffer(bb)) {
			System.err.println("Size mismatch between population result and channel size: " + file.getAbsolutePath());
			return;
		}

		type = master.getTag();
		fileName = file.getAbsolutePath();
	}

	private boolean isValidIff(ByteBuffer buffer, int size) {
		buffer.mark();

		byte[] tag = new byte[4];
		buffer.get(tag);
		String root = new String(tag, StandardCharsets.UTF_8);
		if (!root.equals("FORM"))
			return false;

		int formSize = buffer.getInt();
		if (size != (formSize) + 8)
			return false;

		buffer.reset();
		return true;
	}

	public IffNode addForm(String tag) {
		return addForm(tag, true);
	}

	public IffNode addForm(String tag, boolean enterForm) {
		IffNode form = new IffNode(tag, true);
		currentForm.addChild(form);

		if (enterForm)
			currentForm = form;

		return form;
	}

	public IffNode addChunk(String tag) {
		IffNode chunk = new IffNode(tag, false);
		currentForm.addChild(chunk);
		return chunk;
	}
	
	public boolean hasNextForm() {
		return currentForm.getNextUnreadForm() != null;
	}

	/**
	 * Enters the next unread form based off of the current form
	 * @return Entered form
	 */
	public IffNode enterNextForm() {
		IffNode next = currentForm.getNextUnreadForm();
		if (next == null)
			return null;

		next.setHasBeenRead(true);
		currentForm = next;

		return next;
	}

	/**
	 * Enters the next unread form based off of the current forms children with the given tag
	 * @param tag Form tag to enter
	 * @return Entered form
	 */
	public IffNode enterForm(String tag) {
		for (IffNode child : currentForm.getChildren()) {
			if (!child.isForm() || child.hasBeenRead())
				continue;

			if (child.getTag().equals(tag) && child != currentForm) {
				currentForm = child;
				child.setHasBeenRead(true);
				return child;
			}
		}
		return null;
	}

	/**
	 * Enters the next unread chunk with the given tag
	 * @param tag Tag of the chunk to enter
	 * @return Entered chunk
	 */
	public IffNode enterChunk(String tag) {
		for (IffNode child : currentForm.getChildren()) {
			if (child.isForm() || child.hasBeenRead())
				continue;

			if (child.getTag().equals(tag)) {
				child.setHasBeenRead(true);
				return child;
			}
		}
		return null;
	}

	/**
	 * Enters the next chunk based off of the current forms children
	 * @return Entered chunk
	 */
	public IffNode enterNextChunk() {
		for (IffNode child : currentForm.getChildren()) {
			if (child.isForm() || child.hasBeenRead())
				continue;

			child.setHasBeenRead(true);
			return child;
		}
		return null;
	}

	public IffNode exitForm() {
		IffNode parent = currentForm.getParent();
		if (parent != null) {
//			System.out.println("Exit form " + currentForm.getTag() + " to enter form " + parent.getTag());
			currentForm = parent;
		}

		return currentForm;
	}
	
	public boolean containsUnreadChunk(String tag) {
		for (IffNode child : currentForm.getChildren()) {
			if (child.isForm() || child.hasBeenRead())
				continue;

			if (child.getTag().equals(tag))
				return true;
		}
		return false;
	}

	public byte[] getData() {
		return master.getBytes();
	}

	public String getType() {
		return type;
	}

	public IffNode getMaster() {
		return master;
	}

	public IffNode getCurrentForm() {
		return currentForm;
	}

	public String getFileName() {
		return fileName;
	}
}