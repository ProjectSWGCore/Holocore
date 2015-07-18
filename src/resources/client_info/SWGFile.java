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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Waverunner on 6/4/2015
 */
public class SWGFile {
	private String type;
	private IffNode master;
	private IffNode currentForm;
	private String fileName;

	public SWGFile() {}

	public SWGFile(String type) {
		this.type = type;
		this.master = new IffNode(type, true);
		this.currentForm = master;
	}

	public void save(File file) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(file, false);
		outputStream.write(getData());
		outputStream.close();
	}

	public void read(File file) throws IOException {
		FileInputStream inputStream = new FileInputStream(file);
		FileChannel channel = inputStream.getChannel();

		int size = (int) channel.size();
		ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
		if (channel.read(bb) != size) {
			System.err.println("Failed to properly read the bytes in file " + file.getAbsolutePath() + "!");
			return;
		}
		// Reading will add bytes to the buffer, so we need to flip it before reading the buffer to IffNode's
		bb.flip();

		master = new IffNode("", true);
		currentForm = master;
		master.populateFromBuffer(bb);
		type = master.getTag();
		fileName = file.getAbsolutePath();
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