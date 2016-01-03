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

import intents.network.InboundPacketIntent;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import resources.control.Intent;
import network.encryption.Compression;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.ObjectController;

public class NetworkClient {
	
	private final Object prevPacketIntentMutex = new Object();
	private final Object outboundMutex = new Object();
	private final InetSocketAddress address;
	private final long networkId;
	private final PacketSender packetSender;
	private Intent prevPacketIntent;
	
	public NetworkClient(InetSocketAddress address, long networkId, PacketSender packetSender) {
		this.address = address;
		this.networkId = networkId;
		this.packetSender = packetSender;
		prevPacketIntent = null;
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void sendPacket(Packet p) {
		byte [] encoded = p.encode().array();
		int decompressedLength = encoded.length;
		boolean compressed = encoded.length >= 16;
		if (compressed) {
			byte [] compressedData = Compression.compress(encoded);
			if (compressedData.length >= encoded.length)
				compressed = false;
			else
				encoded = compressedData;
		}
		ByteBuffer data = ByteBuffer.allocate(encoded.length + 5).order(ByteOrder.LITTLE_ENDIAN);
		byte bitmask = 0;
		bitmask |= (compressed?1:0) << 0; // Compressed
		bitmask |= 1 << 1; // SWG
		data.put(bitmask);
		data.putShort((short) encoded.length);
		data.putShort((short) decompressedLength);
		data.put(encoded);
		synchronized (outboundMutex) {
			packetSender.sendPacket(address, data.array());
		}
	}
	
	public boolean process(byte [] data) {
		List <Packet> packets = processPackets(ByteBuffer.wrap(data));
		for (Packet p : packets) {
			p.setAddress(address.getAddress());
			p.setPort(address.getPort());
			synchronized (prevPacketIntentMutex) {
				InboundPacketIntent i = new InboundPacketIntent(p, networkId);
				i.broadcastAfterIntent(prevPacketIntent);
				prevPacketIntent = i;
			}
		}
		return packets.size() > 0;
	}
	
	private List<Packet> processPackets(ByteBuffer data) {
		List <Packet> packets = new ArrayList<>();
		boolean added = true;
		while (added && data.remaining() > 0) {
			added = processPacket(packets, data);
		}
		return packets;
	}
	
	private boolean processPacket(List<Packet> packets, ByteBuffer data) {
		if (data.remaining() < 5) {
			System.err.println("Not enough remaining data for header! Remaining: " + data.remaining());
			return false;
		}
		data.order(ByteOrder.LITTLE_ENDIAN);
		byte bitfield = data.get();
		boolean compressed = (bitfield & (1<<0)) != 0;
		boolean swg = (bitfield & (1<<1)) != 0;
		int length = data.getShort();
		int decompressedLength = data.getShort();
		if (data.remaining() < length) {
			System.err.println("Not enough remaining data! Remaining: " + data.remaining() + "  Length: " + length);
			return false;
		}
		byte [] pData = new byte[length];
		data.get(pData);
		if (compressed) {
			pData = Compression.decompress(pData, decompressedLength);
			length = pData.length;
		}
		if (swg) {
			if (length < 6) {
				System.err.println("Length too small: " + length);
				return false;
			}
			ByteBuffer pBuffer = ByteBuffer.wrap(pData).order(ByteOrder.LITTLE_ENDIAN);
			int crc = pBuffer.getInt(2);
			if (crc == 0x80CE5E46)
				packets.add(ObjectController.decodeController(pBuffer));
			else {
				SWGPacket packet = PacketType.getForCrc(crc);
				packet.decode(pBuffer);
				packets.add(packet);
			}
		}
		return true;
	}
	
	public String toString() {
		return "NetworkClient["+address+"]";
	}
	
}
