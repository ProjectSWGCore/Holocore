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
package com.projectswg.holocore.resources.support.data.persistable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.InputPersistenceStream;
import com.projectswg.common.persistable.OutputPersistenceStream;
import com.projectswg.common.persistable.Persistable;

@RunWith(JUnit4.class)
public class TestSimplePersistable {
	
	@Test
	public void testInputPersistenceStream() throws IOException {
		InputPersistenceStream is = new InputPersistenceStream(new ByteArrayInputStream(createBuffer()));
		PersistableObject po = is.read(PersistableObject::create);
		Assert.assertEquals(13, po.getSomeInt());
		is.close();
	}
	
	@Test
	public void testOutputPersistenceStream() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
		OutputPersistenceStream os = new OutputPersistenceStream(baos);
		PersistableObject po = new PersistableObject(13);
		os.write(po);
		Assert.assertArrayEquals(createBuffer(), baos.toByteArray());
		os.close();
	}
	
	@Test
	public void testPersistenceStreams() throws IOException {
		File tmp = File.createTempFile("PersistableTest", "odb");
		OutputPersistenceStream os = new OutputPersistenceStream(new FileOutputStream(tmp));
		PersistableObject original = new PersistableObject(17);
		os.write(original);
		os.close();
		InputPersistenceStream is = new InputPersistenceStream(new FileInputStream(tmp));
		PersistableObject recreated = is.read(PersistableObject::create);
		is.close();
		Assert.assertEquals(original.getSomeInt(), recreated.getSomeInt());
	}
	
	@Test
	public void testPersistenceStreamMultiple() throws IOException {
		File tmp = File.createTempFile("PersistableTest", "odb");
		OutputPersistenceStream os = new OutputPersistenceStream(new FileOutputStream(tmp));
		PersistableObject original = new PersistableObject(17);
		os.write(original);
		os.write(original);
		os.write(original);
		os.close();
		InputPersistenceStream is = new InputPersistenceStream(new FileInputStream(tmp));
		Assert.assertEquals(original.getSomeInt(), is.read(PersistableObject::create).getSomeInt());
		Assert.assertEquals(original.getSomeInt(), is.read(PersistableObject::create).getSomeInt());
		Assert.assertEquals(original.getSomeInt(), is.read(PersistableObject::create).getSomeInt());
		is.close();
	}
	private byte [] createBuffer() {
		NetBufferStream stream = new NetBufferStream(8);
		stream.addInt(4);
		stream.addInt(13);
		byte [] array = stream.array();
		stream.close();
		return array;
	}
	
	private static class PersistableObject implements Persistable {
		
		private final int someInt;
		
		public PersistableObject(int someInt) {
			this.someInt = someInt;
		}
		
		@Override
		public void save(NetBufferStream stream) {
			stream.addInt(someInt);
		}
		
		@Override
		public void read(NetBufferStream stream) {
			
		}
		
		public static PersistableObject create(NetBufferStream stream) {
			return new PersistableObject(stream.getInt());
		}
		
		public int getSomeInt() {
			return someInt;
		}
		
	}
	
}
