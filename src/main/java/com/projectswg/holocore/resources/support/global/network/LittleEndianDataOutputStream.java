/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.global.network;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LittleEndianDataOutputStream extends OutputStream {  // phew, what a mouthful
	
	private final DataOutputStream stream;
	
	public LittleEndianDataOutputStream(OutputStream out) {
		this.stream = new DataOutputStream(out);
	}
	
	public void write(@NotNull byte [] b) throws IOException {
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
