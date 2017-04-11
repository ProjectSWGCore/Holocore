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
package services.network;

import java.util.ArrayDeque;
import java.util.Queue;

import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.holo.HoloConnectionStarted;
import network.packets.swg.holo.HoloConnectionStopped;
import network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import network.packets.swg.holo.HoloPacket;
import network.packets.swg.holo.HoloSetProtocolVersion;

import com.projectswg.common.debug.Log;

public class HolocoreSessionManager {
	
	private static final String PROTOCOL = "2016-04-13";
	
	private final Queue<Packet> outbound;
	private HolocoreSessionCallback callback;
	private Status status;
	
	public HolocoreSessionManager() {
		outbound = new ArrayDeque<>();
		callback = null;
		status = Status.DISCONNECTED;
	}
	
	public void setCallback(HolocoreSessionCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Called when a packet is being received
	 * @param p the packet received
	 * @return TRUE if the packet is allowed to be broadcasted, FALSE otherwise
	 */
	public ResponseAction onInbound(Packet p) {
		boolean holoPacket = p instanceof HoloPacket;
		if (!holoPacket && getConnectionStatus() != Status.CONNECTED) {
			addToOutbound(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
			return ResponseAction.SHUT_DOWN;
		} else if (holoPacket) {
			if (p instanceof HoloSetProtocolVersion) {
				return processSetProtocolVersion((HoloSetProtocolVersion) p);
			}
		}
		return ResponseAction.CONTINUE;
	}
	
	private ResponseAction processSetProtocolVersion(HoloSetProtocolVersion packet) {
		if (!packet.getProtocol().equals(PROTOCOL)) {
			addToOutbound(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
			return ResponseAction.SHUT_DOWN;
		}
		updateStatus(Status.CONNECTED);
		addToOutbound(new HoloConnectionStarted());
		return ResponseAction.CONTINUE;
	}
	
	/**
	 * Called when a packet is being sent out
	 * @param p the packet being sent
	 * @return TRUE if the packet is allowed to be sent, FALSE otherwise
	 */
	public ResponseAction onOutbound(Packet p) {
		return ResponseAction.CONTINUE;
	}
	
	public Packet [] getOutbound() {
		synchronized (outbound) {
			Packet [] ret = outbound.toArray(new Packet[outbound.size()]);
			outbound.clear();
			return ret;
		}
	}
	
	public Status getConnectionStatus() {
		return status;
	}
	
	public void onSessionCreated() {
		updateStatus(Status.CONNECTING);
	}
	
	public void onSessionDestroyed() {
		updateStatus(Status.DISCONNECTED);
	}
	
	private void addToOutbound(Packet p) {
		synchronized (outbound) {
			outbound.add(p);
		}
	}
	
	private void updateStatus(Status newStatus) {
		Status oldStatus = this.status;
		this.status = newStatus;
		try {
			if (callback != null)
				callback.onSessionStatusChanged(oldStatus, newStatus);
		} catch (Exception e) {
			Log.e(e);
		}
	}
	
	public enum Status {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		SHUT_DOWN
	}
	
	public enum ResponseAction {
		CONTINUE,
		IGNORE,
		SHUT_DOWN
	}
	
	public interface HolocoreSessionCallback {
		void onSessionStatusChanged(Status oldStatus, Status newStatus);
		void onSessionError(String error);
	}
	
}
