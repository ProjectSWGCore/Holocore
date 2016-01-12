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
package intents.network;

import resources.control.Intent;
import resources.network.DisconnectReason;
import resources.player.Player;

public class ForceDisconnectIntent extends Intent {
	
	public static final String TYPE = "ForceDisconnectIntent";
	
	private Player player;
	private boolean disappearImmediately;
	private DisconnectReason reason;
	
	public ForceDisconnectIntent(Player player) {
		this(player, false);
	}
	
	public ForceDisconnectIntent(Player player, boolean disappearImmediately) {
		this(player, disappearImmediately, DisconnectReason.APPLICATION);
	}
	
	public ForceDisconnectIntent(Player player, boolean disappearImmediately, DisconnectReason reason) {
		super(TYPE);
		setPlayer(player);
		setDisappearImmediately(disappearImmediately);
		setDisconnectReason(reason);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setDisappearImmediately(boolean disappearImmediately) {
		this.disappearImmediately = disappearImmediately;
	}
	
	public void setDisconnectReason(DisconnectReason reason) {
		this.reason = reason;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean getDisappearImmediately() {
		return disappearImmediately;
	}
	
	public DisconnectReason getDisconnectReason() {
		return reason;
	}
	
}
