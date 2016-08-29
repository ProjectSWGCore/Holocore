/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.persistable;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import resources.network.NetBufferStream;

public class OutputPersistenceStream extends OutputStream implements Closeable {
	
	private final OutputStream os;
	
	public OutputPersistenceStream(OutputStream os) {
		this.os = os;
	}
	
	@Override
	public void write(int b) {
		throw new UnsupportedOperationException("Unable to write raw data");
	}
	
	@Override
	public void write(byte[] b) {
		throw new UnsupportedOperationException("Unable to write raw data");
	}
	
	@Override
	public void write(byte[] b, int off, int len) {
		throw new UnsupportedOperationException("Unable to write raw data");
	}
	
	public void write(Persistable p) throws IOException {
		NetBufferStream buffer = new NetBufferStream();
		p.save(buffer);
		writeInt(buffer.size());
		os.write(buffer.array(), 0, buffer.size());
		flush();
		buffer.close();
	}
	
	public <T extends Persistable> void write(T obj, PersistableSaver<T> saver) throws IOException {
		NetBufferStream buffer = new NetBufferStream();
		saver.save(obj, buffer);
		writeInt(buffer.size());
		os.write(buffer.array(), 0, buffer.size());
		flush();
		buffer.close();
	}
	
	@Override
	public void flush() throws IOException {
		os.flush();
	}
	
	@Override
	public void close() throws IOException {
		os.close();
	}
	
	private void writeInt(int i) throws IOException {
		os.write(i >>> 0);
		os.write(i >>> 8);
		os.write(i >>> 16);
		os.write(i >>> 24);
	}
	
	public static interface PersistableSaver<T extends Persistable> {
		void save(T obj, NetBufferStream stream);
	}
	
}
