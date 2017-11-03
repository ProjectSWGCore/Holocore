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

import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped;
import com.projectswg.common.network.packets.swg.holo.HoloPacket;
import com.projectswg.common.network.packets.swg.holo.HoloSetProtocolVersion;

import services.network.HolocoreSessionManager.HolocoreSessionException.SessionExceptionReason;

public class HolocoreSessionManager {
	
	private static final String PROTOCOL = "2016-04-13";
	
	private HolocoreSessionCallback callback;
	private SessionStatus status;
	
	public HolocoreSessionManager() {
		callback = null;
		status = SessionStatus.DISCONNECTED;
	}
	
	public void setCallback(HolocoreSessionCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Called when a SWGPacket is being received
	 * @param p the SWGPacket received
	 */
	public void onInbound(SWGPacket p) throws HolocoreSessionException {
		boolean holoPacket = p instanceof HoloPacket;
		
		if (!holoPacket && getStatus() != SessionStatus.CONNECTED)
			throw new HolocoreSessionException(SessionExceptionReason.NO_PROTOCOL);
		
		if (holoPacket) {
			if (p instanceof HoloSetProtocolVersion) {
				processSetProtocolVersion((HoloSetProtocolVersion) p);
			} else if (p instanceof HoloConnectionStopped)
				throw new HolocoreSessionException(SessionExceptionReason.DISCONNECT_REQUESTED);
		}
	}
	
	private void processSetProtocolVersion(HoloSetProtocolVersion p) throws HolocoreSessionException {
		if (!p.getProtocol().equals(PROTOCOL))
			throw new HolocoreSessionException(SessionExceptionReason.PROTOCOL_INVALID);
		
		updateStatus(SessionStatus.CONNECTED);
		try {
			if (callback != null)
				callback.onSessionInitialized();
		} catch (Throwable t) {
			Log.e(t);
		}
	}
	
	public void onSessionCreated() {
		updateStatus(SessionStatus.CONNECTING);
	}
	
	public void onSessionDestroyed() {
		updateStatus(SessionStatus.DISCONNECTED);
	}
	
	public SessionStatus getStatus() {
		return status;
	}
	
	private void updateStatus(SessionStatus newStatus) {
		this.status = newStatus;
	}
	
	private enum SessionStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		CLOSED
	}
	
	public interface HolocoreSessionCallback {
		void onSessionInitialized();
	}
	
	public static class HolocoreSessionException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		private final SessionExceptionReason reason;
		
		public HolocoreSessionException(SessionExceptionReason reason) {
			this.reason = reason;
		}
		
		public SessionExceptionReason getReason() {
			return reason;
		}
		
		public enum SessionExceptionReason {
			NO_PROTOCOL,
			PROTOCOL_INVALID,
			DISCONNECT_REQUESTED
		}
		
	}
	
}
