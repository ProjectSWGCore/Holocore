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

import utilities.ByteUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Waverunner on 6/9/2015
 */
public abstract class DataFactory {
	private static DataFactory instance;

	public DataFactory() {
		instance = this;
	}

	private ClientData getFileType(ByteBuffer bb) {
		bb.position(8); // Skip the first FORM and data size
		String type = ByteUtilities.nextString(bb); // Get the specific form for this IFF so we know what visitor to use

		ClientData data = createDataObject(type);

		return data;
	}

	// readFile only called if dataMap doesn't contain the file as a key or it's value is null
	protected ClientData readFile(String file) {
		FileInputStream stream = null;
		try {
			File f = new File(getFolder() + file);
			if (!f.exists()) {
				System.out.println(file + " not found!");
				return null;
			}

			stream = new FileInputStream(f);
			ByteBuffer bb = readIntoBuffer(stream);

			ClientData visitor = getFileType(bb);
			if (visitor == null)
				return null;

			bb.position(8); // Skip first FORM, some forms only have a node and not a parent form
			parseData(bb, visitor);
			return visitor;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private void parseData(ByteBuffer bb, ClientData visitor) throws Exception {
		String name;
		while (bb.hasRemaining()) {
			name = ByteUtilities.nextString(bb);
			if (name.contains("FORM")) {
				parseFolder(bb, visitor);
			} else {
				parseNode(name, bb, visitor);
			}
		}

	}

	private void parseNode(String name, ByteBuffer bb, ClientData visitor) throws Exception {
		int size = Integer.reverseBytes(bb.getInt()); // Size of this node/folder

		if (size == 0)
			return;

		// Create a new buffer for parsing data specific to this node (excluding name bytes)
		visitor.parse(name, getData(bb, size), size);

	}

	private void parseFolder(ByteBuffer bb, ClientData visitor) throws Exception {
		ByteBuffer data = getData(bb, Integer.reverseBytes(bb.getInt()));

		// Notify visitor of the Folder? May be needed at some point.

		parseData(data, visitor);
	}

	private ByteBuffer readIntoBuffer(FileInputStream stream) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(stream.available()).order(ByteOrder.LITTLE_ENDIAN);
		stream.read(bb.array());
		return bb;
	}

	// Cut down a ByteBuffer from the current position so we are only working with a certain amount of bytes. Useful when we
	// call the ClientData.parse method.
	private ByteBuffer getData(ByteBuffer source, int size) {
		ByteBuffer data = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		int offset = source.position();
		data.put(source.array(), offset, size);
		source.position(offset+size);
		data.flip();

		return data;
	}

	protected abstract ClientData createDataObject(String type);
	protected abstract String getFolder();

	protected static DataFactory getInstance() {
		return instance;
	}
}
