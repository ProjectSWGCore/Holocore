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
package services.galaxy;

import intents.PlayerEventIntent;
import intents.network.CloseConnectionIntent;
import intents.network.ForceDisconnectIntent;
import intents.network.GalacticPacketIntent;
import intents.player.ZonePlayerSwapIntent;
import network.packets.soe.Disconnect;
import network.packets.soe.Disconnect.DisconnectReason;
import network.packets.swg.zone.HeartBeat;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerFlags;
import resources.player.PlayerState;
import resources.server_info.Log;
import utilities.ThreadUtilities;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionService extends Service {
	
	private static final double LD_THRESHOLD = TimeUnit.MINUTES.toMillis(3); // Time since last packet
	private static final double DISAPPEAR_THRESHOLD = TimeUnit.MINUTES.toMillis(2); // Time after the LD
	
	private final ScheduledExecutorService updateService;
	private final Runnable updateRunnable;
	private final Runnable disappearRunnable;
	private final Set <DisappearPlayer> disappearPlayers;
	private final Set <Player> zonedInPlayers;
	
	public ConnectionService() {
		updateService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("conn-update-service"));
		zonedInPlayers = new LinkedHashSet<Player>();
		disappearPlayers = new HashSet<DisappearPlayer>();
		updateRunnable = new Runnable() {
			public void run() {
				synchronized (zonedInPlayers) {
					Iterator<Player> i = zonedInPlayers.iterator();
					while (i.hasNext()) {
						Player p = i.next();
						if (p.getTimeSinceLastPacket() > LD_THRESHOLD) {
							i.remove();
							logOut(p);
							disconnect(p, DisconnectReason.TIMEOUT);
						}
					}
				}
			}
		};
		disappearRunnable = new Runnable() {
			public void run() {
				synchronized (disappearPlayers) {
					Iterator<DisappearPlayer> iter = disappearPlayers.iterator();
					while (iter.hasNext()) {
						DisappearPlayer p = iter.next();
						if ((System.nanoTime()-p.getTime())/1E6 >= DISAPPEAR_THRESHOLD) {
							disappear(p.getPlayer(), DisconnectReason.TIMEOUT);
							iter.remove();
						}
					}
				}
			}
		};
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(ForceDisconnectIntent.TYPE);
		registerForIntent(ZonePlayerSwapIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		updateService.scheduleAtFixedRate(updateRunnable, 10, 10, TimeUnit.SECONDS);
		return super.start();
	}
	
	@Override
	public boolean terminate() {
		updateService.shutdownNow();
		boolean success = false;
		try {
			success = updateService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return super.terminate() && success;
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof PlayerEventIntent)
			onPlayerEventIntent((PlayerEventIntent) i);
		else if (i instanceof GalacticPacketIntent)
			onGalacticPacketIntent((GalacticPacketIntent) i);
		else if (i instanceof ForceDisconnectIntent)
			onForceDisconnectIntent((ForceDisconnectIntent) i);
		else if (i instanceof ZonePlayerSwapIntent)
			onZonePlayerSwapIntent((ZonePlayerSwapIntent) i);
	}
	
	private void onPlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE: {
				Player p = pei.getPlayer();
				synchronized (zonedInPlayers) {
					zonedInPlayers.add(p);
				}
				break;
			}
			default:
				break;
		}
	}
	
	private void onGalacticPacketIntent(GalacticPacketIntent gpi) {
		if (gpi.getPacket() instanceof HeartBeat) {
			Player p = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
			if (p != null)
				p.sendPacket(gpi.getPacket());
		} else if (gpi.getPacket() instanceof Disconnect) {
			Player p = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
			if (p != null) {
				if (p.getPlayerState() != PlayerState.DISCONNECTED) {
					logOut(p);
					disconnect(p, DisconnectReason.TIMEOUT);
				} else {
					disconnect(p, DisconnectReason.OTHER_SIDE_TERMINATED);
				}
			}
		}
	}
	
	private void onForceDisconnectIntent(ForceDisconnectIntent fdi) {
		logOut(fdi.getPlayer(), !fdi.getDisappearImmediately());
		disconnect(fdi.getPlayer(), fdi.getDisconnectReason());
		if (fdi.getDisappearImmediately())
			disappear(fdi.getPlayer(), fdi.getDisconnectReason());
	}
	
	private void onZonePlayerSwapIntent(ZonePlayerSwapIntent zpsi) {
		Player before = zpsi.getBeforePlayer();
		Player after = zpsi.getAfterPlayer();
		CreatureObject creature = zpsi.getCreature();
		removeFromLists(before);
		updatePlayTime(before);
		creature.getPlayerObject().clearFlagBitmask(PlayerFlags.LD);
		Log.i("ConnectionService", "Logged out %s with character %s", before.getUsername(), before.getCharacterName());
		new PlayerEventIntent(before, before.getGalaxyName(), PlayerEvent.PE_LOGGED_OUT).broadcast();
		Log.i("ConnectionService", "Disconnected %s with character %s and reason: %s", before.getUsername(), before.getCharacterName(), DisconnectReason.NEW_CONNECTION_ATTEMPT);
		new CloseConnectionIntent(before.getConnectionId(), before.getNetworkId(), DisconnectReason.NEW_CONNECTION_ATTEMPT).broadcast();
		before.setPlayerState(PlayerState.DISCONNECTED);
		before.setCreatureObject(null);
		creature.setOwner(after);
	}
	
	private void removeFromLists(Player player) {
		synchronized (zonedInPlayers) {
			zonedInPlayers.remove(player);
		}
		removeFromDisappear(player);
	}

	private void removeFromDisappear(Player player) {
		synchronized (disappearPlayers) {
			Iterator <DisappearPlayer> disappearIterator = disappearPlayers.iterator();
			while (disappearIterator.hasNext()) {
				DisappearPlayer old = disappearIterator.next();
				Player oldPlayer = old.getPlayer();
				CreatureObject oldObj = old.getPlayer().getCreatureObject();
				if (oldObj == null || player.equals(old) || player == oldPlayer) {
					disappearIterator.remove();
				}
			}
		}
	}
	
	private void logOut(Player p) {
		logOut(p, true);
	}
	
	private void logOut(Player p, boolean addToDisappear) {
		removeFromLists(p);
		Log.i("ConnectionService", "Logged out %s with character %s", p.getUsername(), p.getCharacterName());
		updatePlayTime(p);
		if (p.getPlayerState() != PlayerState.LOGGED_OUT)
			System.out.println("[" + p.getUsername() +"] Logged out " + p.getCharacterName());
		if (p.getPlayerObject() != null)
			p.getPlayerObject().setFlagBitmask(PlayerFlags.LD);
		p.setPlayerState(PlayerState.LOGGED_OUT);
		new PlayerEventIntent(p, p.getGalaxyName(), PlayerEvent.PE_LOGGED_OUT).broadcast();
		if (addToDisappear) {
			synchronized (disappearPlayers) {
				disappearPlayers.add(new DisappearPlayer(System.nanoTime(), p));
			}
			updateService.schedule(disappearRunnable, (long) DISAPPEAR_THRESHOLD, TimeUnit.MILLISECONDS);
		}
	}
	
	private void disappear(Player p, DisconnectReason reason) {
		Log.i("ConnectionService", "Disappeared %s with character %s", p.getUsername(), p.getCharacterName());
		if (p.getPlayerObject() != null)
			p.getPlayerObject().clearFlagBitmask(PlayerFlags.LD);

		switch(reason) {
			case NEW_CONNECTION_ATTEMPT: // The player is attempting to re-zone
				removeFromDisappear(p);
				break;
			default:
				removeFromLists(p);
				p.getCreatureObject().setOwner(null);
				break;
		}
		p.setPlayerState(PlayerState.DISCONNECTED);
		System.out.println("[" + p.getUsername() +"] " + p.getCharacterName() + " disappeared");
		new PlayerEventIntent(p, PlayerEvent.PE_DISAPPEAR).broadcast();
	}
	
	private void disconnect(Player player, DisconnectReason reason) {
		Log.i("ConnectionService", "Disconnected %s with character %s and reason: %s", player.getUsername(), player.getCharacterName(), reason);
		new CloseConnectionIntent(player.getConnectionId(), player.getNetworkId(), reason).broadcast();
	}
	
	private void updatePlayTime(Player p) {
		PlayerObject playerObject = p.getPlayerObject();
		if (playerObject == null)
			return;
		
		int currentTime = playerObject.getPlayTime();
		int startTime = playerObject.getStartPlayTime();
		int deltaTime = (int) ((System.currentTimeMillis()) - startTime);
		int newTotalTime = currentTime + (int) TimeUnit.MILLISECONDS.toSeconds(deltaTime);
		playerObject.setPlayTime(newTotalTime);
	}
	
	private static class DisappearPlayer {
		private final long time;
		private final Player player;
		
		public DisappearPlayer(long time, Player player) {
			this.time = time;
			this.player = player;
		}
		
		public long getTime() { return time; }
		public Player getPlayer() { return player; }
		
		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (!(o instanceof DisappearPlayer))
				return false;
			return ((DisappearPlayer) o).getPlayer().equals(player);
		}
		
		@Override
		public int hashCode() {
			return player.hashCode();
		}
	}
	
}
