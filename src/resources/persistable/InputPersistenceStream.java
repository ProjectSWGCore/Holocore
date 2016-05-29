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

import java.io.IOException;
import java.io.InputStream;

import resources.network.NetBufferStream;

public class InputPersistenceStream extends InputStream {
	
	private final InputStream is;
	
	public InputPersistenceStream(InputStream is) {
		this.is = is;
	}
	
	public int read() {
		throw new UnsupportedOperationException("Unable to read raw data");
	}
	
	public int read(byte[] b) {
		throw new UnsupportedOperationException("Unable to read raw data");
	}
	
	public int read(byte[] b, int off, int len) {
		throw new UnsupportedOperationException("Unable to read raw data");
	}
	
	public <T extends Persistable> T read(PersistableCreator<T> creator) throws IOException {
		int size = readInt();
		try (NetBufferStream stream = new NetBufferStream(size)) {
			byte [] buffer = new byte[Math.min(size, 2048)];
			int pos = 0;
			while (pos < size) {
				int n = is.read(buffer, 0, Math.min(size-pos, buffer.length));
				if (n == -1)
					break;
				stream.write(buffer, 0, n);
				pos += n;
			}
			stream.position(0);
			return creator.create(stream);
		}
	}
	
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
	
	public int available() throws IOException {
		return is.available();
	}
	
	public void close() throws IOException {
		is.close();
	}
	
	public void mark(int readlimit) {
		is.mark(readlimit);
	}
	
	public void reset() throws IOException {
		is.reset();
	}
	
	public boolean markSupported() {
		return is.markSupported();
	}
	
	private int readInt() throws IOException {
		int ret = 0;
		int b;
		for (int i = 0; i < 4; i++) {
			b = is.read();
			if (b != -1)
				ret |= b << (i * 8);
		}
		return ret;
	}
	
	public static interface PersistableCreator<T extends Persistable> {
		T create(NetBufferStream stream);
	}
	
}
