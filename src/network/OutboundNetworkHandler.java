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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import network.encryption.Encryption;
import network.packets.Packet;
import network.packets.soe.DataChannelA;
import network.packets.soe.Fragmented;
import network.packets.soe.MultiPacket;
import network.packets.soe.SessionRequest;
import network.packets.soe.SessionResponse;
import network.packets.swg.SWGPacket;

public class OutboundNetworkHandler {
	
	private static final long RESEND_TIMEOUT = TimeUnit.MILLISECONDS.toMillis(3000);
	
	private final Queue <byte []> assembleQueue;
	private final Queue <SequencedPacket> sequenced;
	private short sendSequence;
	private int crc;
	
	public OutboundNetworkHandler() {
		assembleQueue = new LinkedList<byte []>();
		sequenced = new PriorityQueue<SequencedPacket>();
		sendSequence = 0;
		crc = 0;
	}
	
	public synchronized void reset() {
		sequenced.clear();
		assembleQueue.clear();
		sendSequence = 0;
		crc = 0;
	}
	
	public synchronized void setCrc(int crc) {
		this.crc = crc;
	}
	
	public synchronized int getCrc() {
		return crc;
	}
	
	public synchronized short getSentSequence() {
		return sendSequence;
	}
	
	public synchronized void onAcknowledge(short sequence) {
		synchronized (sequenced) {
			Iterator <SequencedPacket> it = sequenced.iterator();
			while(it.hasNext()) {
				SequencedPacket sp = it.next();
				if (sp.getSequence() <= sequence) {
					it.remove();
				} else {
					if (sp.isSent())
						sp.setSent(false);
					else
						break;
				}
			}
		}
	}
	
	public synchronized void onOutOfOrder(short sequence) {
		synchronized (sequenced) {
			Iterator <SequencedPacket> it = sequenced.iterator();
			while (it.hasNext()) {
				SequencedPacket sp = it.next();
				if (sp.getSequence() > sequence) {
					if (sp.isSent())
						sp.setSent(false);
					else
						break;
				} else if (!sp.isSent() || sp.getSequence() == sequence) {
					pushAssembledUnencrypted(sp.getPacket()); // Pre-encrypted before putting into list
					sp.setSent(true);
					sp.updateSent();
				}
			}
		}
	}
	
	public synchronized boolean hasAssembled() {
		synchronized (assembleQueue) {
			return !assembleQueue.isEmpty();
		}
	}
	
	public synchronized byte [] pollAssembled() {
		synchronized (assembleQueue) {
			return assembleQueue.poll();
		}
	}
	
	public synchronized void resendOldUnacknowledged() {
		synchronized (sequenced) {
			for (SequencedPacket packet : sequenced) {
				if (packet.hasBeen(RESEND_TIMEOUT)) {
					pushAssembledUnencrypted(packet.getPacket()); // Pre-encrypted before putting into list
					packet.setSent(true);
					packet.updateSent();
				}
			}
		}
	}
	
	private byte [] pushAssembledEncrypted(byte [] data) {
		data = Encryption.encode(data, crc);
		synchronized (assembleQueue) {
			assembleQueue.add(data);
		}
		return data;
	}
	
	private void pushAssembledUnencrypted(byte [] data) {
		synchronized (assembleQueue) {
			assembleQueue.add(data);
		}
	}
	
	private void pushSequencedPacket(short sequence, byte [] packet) {
		synchronized (sequenced) {
			sequenced.add(new SequencedPacket(sequence, packet));
		}
	}
	
	public synchronized int assemble(Packet packet) {
		if (packet instanceof SessionRequest || packet instanceof SessionResponse) {
			pushAssembledUnencrypted(packet.encode().array());
			return 1;
		} else
			return assembleUnencrypted(packet);
	}
	
	private int assembleUnencrypted(Packet packet) {
		if (packet instanceof SWGPacket)
			return assembleSwg((SWGPacket) packet);
		else
			return assembleSoe(packet);
	}
	
	private int assembleSoe(Packet packet) {
		if (packet instanceof DataChannelA)
			return assembleDataChannelA((DataChannelA) packet);
		if (packet instanceof MultiPacket)
			return assembleMultiPacket((MultiPacket) packet);
		pushAssembledEncrypted(packet.encode().array());
		return 1;
	}
	
	private int assembleSwg(SWGPacket packet) {
		return assembleDataChannelA(new DataChannelA(packet));
	}
	
	private int assembleMultiPacket(MultiPacket m) {
		int len = m.getLength();
		if (len >= 493) {
			int count = getFragmentedPacketCount(len);
			int lastSeq = updateSequencesMulti((short)(sendSequence+count), m);
			for (Fragmented f : Fragmented.encode(m.encode(), sendSequence)) {
				byte [] encoded = f.encode().array();
				byte [] encrypted = pushAssembledEncrypted(encoded);
				pushSequencedPacket(f.getSequence(), encrypted);
			}
			sendSequence = (short) (lastSeq + 1);
			return count;
		} else {
			sendSequence = updateSequencesMulti(sendSequence, m);
			pushAssembledEncrypted(m.encode().array());
			return 1;
		}
	}
	
	private int assembleDataChannelA(DataChannelA d) {
		int len = d.getLength();
		if (len >= 493) {
			int count = getFragmentedPacketCount(len);
			int lastSeq = updateSequenceData((short)(sendSequence+count), d);
			for (Fragmented f : Fragmented.encode(d.encode(), sendSequence)) {
				byte [] encoded = f.encode().array();
				byte [] encrypted = pushAssembledEncrypted(encoded);
				pushSequencedPacket(f.getSequence(), encrypted);
			}
			sendSequence = (short) lastSeq;
			return count;
		} else {
			d.setSequence(sendSequence++);
			byte [] encoded = d.encode().array();
			byte [] encrypted = pushAssembledEncrypted(encoded);
			pushSequencedPacket(d.getSequence(), encrypted);
			return 1;
		}
	}
	
	private short updateSequencesMulti(short seq, MultiPacket m) {
		for (Packet p : m.getPackets()) {
			if (p instanceof DataChannelA)
				seq = updateSequenceData(seq, (DataChannelA) p);
		}
		return seq;
	}
	
	private short updateSequenceData(short seq, DataChannelA d) {
		d.setSequence(seq++);
		return seq;
	}
	
	private int getFragmentedPacketCount(int length) {
		return (int) Math.ceil((length+4)/489.0);
	}
	
	private static class SequencedPacket implements Comparable <SequencedPacket> {
		private final short sequence;
		private final byte [] packet;
		private long sentTime;
		private boolean sent;
		
		public SequencedPacket(short sequence, byte [] packet) {
			this.sequence = sequence;
			this.packet = packet;
			this.sentTime = System.nanoTime();
			sent = false;
		}
		
		public short getSequence() {
			return sequence;
		}
		
		public byte [] getPacket() {
			return packet;
		}
		
		public boolean isSent() {
			return sent;
		}
		
		public void setSent(boolean sent) {
			this.sent = sent;
		}
		
		public boolean hasBeen(double milliseconds) {
			return hasBeen() >= milliseconds;
		}
		
		public double hasBeen() {
			return (System.nanoTime() - sentTime) / 1E6;
		}
		
		public void updateSent() {
			sentTime = System.nanoTime();
		}
		
		@Override
		public int compareTo(SequencedPacket sp) {
			if (sequence < sp.getSequence())
				return -1;
			if (sequence == sp.getSequence())
				return 0;
			return 1;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (o instanceof SequencedPacket)
				return ((SequencedPacket) o).getSequence() == sequence;
			return false;
		}
		
		@Override
		public int hashCode() {
			return sequence;
		}
		
	}
	
}
