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

import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import resources.network.DisconnectReason;
import resources.network.NetBufferStream;
import utilities.IntentChain;
import network.encryption.Compression;
import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.SWGPacket;
import network.packets.swg.holo.HoloPacket;
import network.packets.swg.zone.object_controller.ObjectController;

public class NetworkClient {
	
	private static final int DEFAULT_BUFFER = 128;
	private static final int TRY_LOCK_TIME = 100;
	
	private final IntentChain intentChain = new IntentChain();
	private final Object bufferMutex = new Object();
	private final ReentrantLock inboundLock = new ReentrantLock(true);
	private final ReentrantLock outboundLock = new ReentrantLock(true);
	private final InetSocketAddress address;
	private final long networkId;
	private final PacketSender packetSender;
	private final Queue<Packet> outboundQueue;
	private final NetBufferStream buffer;
	private ClientStatus status;
	
	public NetworkClient(InetSocketAddress address, long networkId, PacketSender packetSender) {
		this.address = address;
		this.networkId = networkId;
		this.packetSender = packetSender;
		this.outboundQueue = new LinkedList<>();
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.status = ClientStatus.DISCONNECTED;
	}
	
	public void close() {
		buffer.reset();
		intentChain.reset();
		outboundQueue.clear();
		status = ClientStatus.DISCONNECTED;
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void onConnected() {
		status = ClientStatus.CONNECTED;
		intentChain.broadcastAfter(new ConnectionOpenedIntent(networkId));
	}
	
	public void onConnecting() {
		status = ClientStatus.CONNECTING;
	}
	
	public void onDisconnected() {
		status = ClientStatus.DISCONNECTED;
	}
	
	public ClientStatus getStatus() {
		return status;
	}
	
	public void processOutbound() {
		if (!tryLockInterruptable(outboundLock))
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
			buffer.write(data);
		}
	}
	
	public boolean processInbound() {
		if (!tryLockInterruptable(inboundLock))
			return false;
		try {
			List <Packet> packets;
			synchronized (bufferMutex) {
				packets = processPackets();
				buffer.compact();
			}
			for (Packet p : packets) {
				p.setAddress(address.getAddress());
				p.setPort(address.getPort());
				if (status != ClientStatus.CONNECTED && !(p instanceof HoloPacket)) {
					addToOutbound(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
					processOutbound();
					new ConnectionClosedIntent(networkId, DisconnectReason.CONNECTION_REFUSED).broadcast();
					break;
				}
				intentChain.broadcastAfter(new InboundPacketIntent(p, networkId));
			}
			return packets.size() > 0;
		} finally {
			inboundLock.unlock();
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
		byte bitfield = buffer.getByte();
		boolean compressed = (bitfield & 0x01) != 0;
		int length = buffer.getShort();
		int decompressedLength = buffer.getShort();
		if (buffer.remaining() < length) {
			buffer.position(buffer.position() - 5);
			throw new EOFException("Not enough remaining data! Remaining: " + buffer.remaining() + "  Length: " + length);
		}
		byte [] pData = buffer.getArray(length);
		if (compressed) {
			pData = Compression.decompress(pData, decompressedLength);
		}
		return processSWG(pData);
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
	
	private boolean tryLockInterruptable(Lock l) {
		try {
			return l.tryLock(TRY_LOCK_TIME, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public String toString() {
		return "NetworkClient["+address+"]";
	}
	
	public enum ClientStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
	}
	
}
