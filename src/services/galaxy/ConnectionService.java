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
import intents.network.GalacticPacketIntent;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.HeartBeat;
import resources.network.DisconnectReason;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerFlags;
import resources.server_info.Log;
import services.CoreManager;
import utilities.ThreadUtilities;

import com.projectswg.common.concurrency.SynchronizedSet;
import com.projectswg.common.control.Intent;
import com.projectswg.common.control.Service;
import com.projectswg.common.debug.Assert;

public class ConnectionService extends Service {
	
	private static final double DISAPPEAR_THRESHOLD = TimeUnit.MINUTES.toMillis(3); // Time after the LD
	
	private final ScheduledExecutorService updateService;
	private final Runnable disappearRunnable;
	private final Set <DisappearPlayer> disappearPlayers;
	private final Set <Player> zonedInPlayers;
	
	public ConnectionService() {
		updateService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("conn-update-service"));
		zonedInPlayers = new SynchronizedSet<>();
		disappearPlayers = new SynchronizedSet<>();
		disappearRunnable = () -> {
			synchronized (disappearPlayers) {
				Iterator<DisappearPlayer> iter = disappearPlayers.iterator();
				while (iter.hasNext()) {
					DisappearPlayer p = iter.next();
					if ((System.nanoTime()-p.getTime())/1E6 >= DISAPPEAR_THRESHOLD) {
						disappear(p.getPlayer(), false, DisconnectReason.APPLICATION);
						iter.remove();
					}
				}
			}
		};
		
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
	}
	
	@Override
	public boolean terminate() {
		updateService.shutdownNow();
		boolean success = false;
		try {
			success = updateService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(e);
		}
		return super.terminate() && success;
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				zoneIn(p);
				break;
			case PE_LOGGED_OUT:
				logOut(p);
				break;
			default:
				break;
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Player p = gpi.getPlayer();
		p.updateLastPacketTimestamp();
		if (gpi.getPacket() instanceof HeartBeat)
			p.sendPacket(gpi.getPacket());
	}
	
	private void setPlayerFlag(Player p, PlayerFlags flag) {
		PlayerObject player = p.getPlayerObject();
		if (player == null)
			return;
		player.setFlagBitmask(flag);
	}
	
	private void clearPlayerFlag(Player p, PlayerFlags flag) {
		PlayerObject player = p.getPlayerObject();
		if (player == null)
			return;
		player.clearFlagBitmask(flag);
	}
	
	private void zoneIn(Player p) {
		CoreManager.getGalaxy().incrementPopulationCount();
		clearPlayerFlag(p, PlayerFlags.LD);
		removeFromDisappear(p);
		Assert.test(zonedInPlayers.add(p));
	}
	
	private void logOut(Player p) {
		if (!zonedInPlayers.remove(p))
			return;
		Log.i("Logged out %s with character %s", p.getUsername(), p.getCharacterName());
		CoreManager.getGalaxy().decrementPopulationCount();
		setPlayerFlag(p, PlayerFlags.LD);
		removeFromDisappear(p);
		updatePlayTime(p);
		addToDisappear(p);
	}
	
	private void disappear(Player p, boolean newConnection, DisconnectReason reason) {
		if (p.getCreatureObject() == null)
			return;
		Log.i("Disappeared %s with character %s with reason %s", p.getUsername(), p.getCharacterName(), reason);
		
		removeFromDisappear(p);
		Intent i = new PlayerEventIntent(p, PlayerEvent.PE_DISAPPEAR);
		new PlayerEventIntent(p, PlayerEvent.PE_DESTROYED).broadcastAfterIntent(i);
		i.broadcast();
	}
	
	private void updatePlayTime(Player p) {
		PlayerObject playerObject = p.getPlayerObject();
		if (playerObject == null)
			return;
		
		playerObject.updatePlayTime();
	}
	
	private void addToDisappear(Player p) {
		disappearPlayers.add(new DisappearPlayer(System.nanoTime(), p));
		updateService.schedule(disappearRunnable, (long) DISAPPEAR_THRESHOLD + 100, TimeUnit.MILLISECONDS);
	}
	
	private void removeFromDisappear(Player player) {
		synchronized (disappearPlayers) {
			Iterator <DisappearPlayer> disappearIterator = disappearPlayers.iterator();
			while (disappearIterator.hasNext()) {
				DisappearPlayer old = disappearIterator.next();
				Player oldPlayer = old.getPlayer();
				CreatureObject oldObj = old.getPlayer().getCreatureObject();
				if (oldObj == null || player.equals(oldPlayer) || player == oldPlayer) {
					disappearIterator.remove();
				}
			}
		}
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
