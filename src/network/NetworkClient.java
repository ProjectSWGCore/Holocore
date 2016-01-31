/***********************************************************************************
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
package network;

import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import resources.control.Intent;
import resources.server_info.Log;
import network.encryption.Compression;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.ObjectController;

public class NetworkClient {
	
	private static final int DEFAULT_BUFFER = 128;
	
	private final Object prevPacketIntentMutex = new Object();
	private final Object bufferMutex = new Object();
	private final ReentrantLock inboundLock = new ReentrantLock(true);
	private final ReentrantLock outboundLock = new ReentrantLock(true);
	private final InetSocketAddress address;
	private final long networkId;
	private final PacketSender packetSender;
	private final Queue<Packet> outboundQueue;
	private Intent prevPacketIntent;
	private ByteBuffer buffer;
	private long lastBufferSizeModification;
	
	public NetworkClient(InetSocketAddress address, long networkId, PacketSender packetSender) {
		this.address = address;
		this.networkId = networkId;
		this.packetSender = packetSender;
		this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER);
		this.outboundQueue = new LinkedList<>();
		lastBufferSizeModification = System.nanoTime();
		prevPacketIntent = null;
	}
	
	public void close() {
		synchronized (bufferMutex) {
			buffer = ByteBuffer.allocate(0);
		}
		synchronized (prevPacketIntentMutex) {
			prevPacketIntent = null;
		}
		outboundQueue.clear();
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void onConnected() {
		synchronized (prevPacketIntentMutex) {
			prevPacketIntent = new ConnectionOpenedIntent(networkId);
			prevPacketIntent.broadcast();
		}
	}
	
	public void processOutbound() {
		if (!outboundLock.tryLock())
			return;
		try {
			Packet p;
			while (!outboundQueue.isEmpty()) {
				p = outboundQueue.poll();
				if (p == null)
					break;
				sendPacket(p);
			}
		} finally {
			outboundLock.unlock();
		}
	}
	
	public void addToOutbound(Packet packet) {
		outboundLock.lock();
		try {
			outboundQueue.add(packet);
		} finally {
			outboundLock.unlock();
		}
	}
	
	public void addToBuffer(byte [] data) {
		synchronized (bufferMutex) {
			if (data.length > buffer.remaining()) { // Increase size
				int nCapacity = buffer.capacity() * 2;
				while (nCapacity < buffer.position()+data.length)
					nCapacity *= 2;
				ByteBuffer bb = ByteBuffer.allocate(nCapacity);
				buffer.flip();
				bb.put(buffer);
				bb.put(data);
				this.buffer = bb;
				lastBufferSizeModification = System.nanoTime();
			} else {
				buffer.put(data);
				if (buffer.position() < buffer.capacity()/4 && (System.nanoTime()-lastBufferSizeModification) >= 1E9)
					shrinkBuffer();
			}
		}
	}
	
	public boolean processInbound() {
		if (!inboundLock.tryLock())
			return false;
		try {
			List <Packet> packets;
			synchronized (bufferMutex) {
				buffer.flip();
				packets = processPackets();
				buffer.compact();
			}
			synchronized (prevPacketIntentMutex) {
				for (Packet p : packets) {
					p.setAddress(address.getAddress());
					p.setPort(address.getPort());
					Log.d("NetworkClient", "Inbound: %s", p.getClass().getSimpleName());
					InboundPacketIntent i = new InboundPacketIntent(p, networkId);
					i.broadcastAfterIntent(prevPacketIntent);
					prevPacketIntent = i;
				}
			}
			return packets.size() > 0;
		} finally {
			inboundLock.unlock();
		}
	}
	
	private void shrinkBuffer() {
		synchronized (bufferMutex) {
			int nCapacity = DEFAULT_BUFFER;
			while (nCapacity < buffer.position())
				nCapacity *= 2;
			if (nCapacity >= buffer.capacity())
				return;
			ByteBuffer bb = ByteBuffer.allocate(nCapacity).order(ByteOrder.LITTLE_ENDIAN);
			buffer.flip();
			bb.put(buffer);
			buffer = bb;
			lastBufferSizeModification = System.nanoTime();
		}
	}
	
	private List<Packet> processPackets() {
		List <Packet> packets = new LinkedList<>();
		Packet p = null;
		try {
			while (buffer.hasRemaining()) {
				p = processPacket();
				if (p != null)
					packets.add(p);
			}
		} catch (EOFException e) {
			System.err.println(e.getMessage());
		}
		return packets;
	}
	
	private Packet processPacket() throws EOFException {
		if (buffer.remaining() < 5)
			throw new EOFException("Not enough remaining data for header! Remaining: " + buffer.remaining());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte bitfield = buffer.get();
		boolean compressed = (bitfield & (1<<0)) != 0;
		boolean swg = (bitfield & (1<<1)) != 0;
		int length = buffer.getShort();
		int decompressedLength = buffer.getShort();
		if (buffer.remaining() < length) {
			buffer.position(buffer.position() - 5);
			throw new EOFException("Not enough remaining data! Remaining: " + buffer.remaining() + "  Length: " + length);
		}
		byte [] pData = new byte[length];
		buffer.get(pData);
		if (compressed) {
			pData = Compression.decompress(pData, decompressedLength);
		}
		if (swg)
			return processSWG(pData);
		else
			return processProtocol(pData);
	}
	
	private Packet processProtocol(byte [] data) {
		return null;
	}
	
	private SWGPacket processSWG(byte [] data) {
		if (data.length < 6) {
			System.err.println("Length too small: " + data.length);
			return null;
		}
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		int crc = buffer.getInt(2);
		if (crc == 0x80CE5E46)
			return ObjectController.decodeController(buffer);
		else {
			SWGPacket packet = PacketType.getForCrc(crc);
			if (packet != null)
				packet.decode(buffer);
			return packet;
		}
	}
	
	private void sendPacket(Packet p) {
		ByteBuffer encoded = p.encode();
		encoded.position(0);
		int decompressedLength = encoded.remaining();
		boolean compressed = decompressedLength >= 50;
		if (compressed) {
			ByteBuffer compress = compress(encoded);
			compressed = compress != encoded;
			encoded = compress;
		}
		Log.d("NetworkClient", "Outbound: %s", p.getClass().getSimpleName());
		sendPacket(encoded, compressed, decompressedLength);
	}
	
	private ByteBuffer compress(ByteBuffer data) {
		ByteBuffer compressedBuffer = ByteBuffer.allocate(Compression.getMaxCompressedLength(data.remaining()));
		int length = Compression.compress(data.array(), compressedBuffer.array());
		compressedBuffer.position(0);
		compressedBuffer.limit(length);
		if (length >= data.remaining())
			return data;
		else
			return compressedBuffer;
	}
	
	private void sendPacket(ByteBuffer packet, boolean compressed, int rawLength) {
		ByteBuffer data = ByteBuffer.allocate(packet.remaining() + 5).order(ByteOrder.LITTLE_ENDIAN);
		byte bitmask = 0;
		bitmask |= (compressed?1:0) << 0; // Compressed
		bitmask |= 1 << 1; // SWG
		data.put(bitmask);
		data.putShort((short) packet.remaining());
		data.putShort((short) rawLength);
		data.put(packet);
		data.flip();
		packetSender.sendPacket(address, data);
	}
	
	public String toString() {
		return "NetworkClient["+address+"]";
	}
	
}
