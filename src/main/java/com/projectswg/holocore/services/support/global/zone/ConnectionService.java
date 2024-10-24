/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.common.network.packets.swg.zone.HeartBeat;
import com.projectswg.common.utilities.ThreadUtilities;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.ForceLogoutIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionService extends Service {
	
	private static final double DISAPPEAR_THRESHOLD = TimeUnit.MINUTES.toMillis(3); // Time after the LD
	
	private final ScheduledExecutorService updateService;
	private final Runnable disappearRunnable;
	private final Set <DisappearPlayer> disappearPlayers;
	private final Set <Player> zonedInPlayers;
	
	public ConnectionService() {
		updateService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("conn-update-service"));
		zonedInPlayers = ConcurrentHashMap.newKeySet();
		disappearPlayers = ConcurrentHashMap.newKeySet();
		disappearRunnable = () -> {
			synchronized (disappearPlayers) {
				Iterator<DisappearPlayer> iter = disappearPlayers.iterator();
				while (iter.hasNext()) {
					DisappearPlayer p = iter.next();
					if ((System.nanoTime()-p.time())/1E6 >= DISAPPEAR_THRESHOLD) {
						disappear(p.player());
						iter.remove();
					}
				}
			}
		};
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
	
	@IntentHandler
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
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		Player p = gpi.getPlayer();
		p.updateLastPacketTimestamp();
		if (gpi.getPacket() instanceof HeartBeat)
			p.sendPacket(gpi.getPacket());
	}
	
	@IntentHandler
	private void handleForceLogoutIntent(ForceLogoutIntent fli) {
		Player player = fli.getPlayer();
		Objects.requireNonNull(player.getCreatureObject(), "ForceLogoutIntent must have a valid player with creature object!");
		logOut(player);
		disappear(player);
	}
	
	private void setPlayerFlag(Player p) {
		PlayerObject player = p.getPlayerObject();
		if (player == null)
			return;
		player.getFlags().set(PlayerFlags.LD);
	}
	
	private void clearPlayerFlag(Player p) {
		PlayerObject player = p.getPlayerObject();
		if (player == null)
			return;
		player.getFlags().clear(PlayerFlags.LD);
	}
	
	private void zoneIn(Player p) {
		ProjectSWG.INSTANCE.getGalaxy().incrementPopulationCount();
		clearPlayerFlag(p);
		removeFromDisappear(p);
		boolean unique = zonedInPlayers.add(p);
		assert unique;
	}
	
	private void logOut(Player p) {
		if (!zonedInPlayers.remove(p))
			return;
		StandardLog.onPlayerEvent(this, p, "logged out");
		ProjectSWG.INSTANCE.getGalaxy().decrementPopulationCount();
		setPlayerFlag(p);
		removeFromDisappear(p);
		addToDisappear(p);
	}
	
	private void disappear(Player p) {
		if (p.getCreatureObject() == null)
			return;
		StandardLog.onPlayerEvent(this, p, "disappeared");
		
		removeFromDisappear(p);
		IntentChain.broadcastChain(
				new PlayerEventIntent(p, PlayerEvent.PE_DISAPPEAR),
				new PlayerEventIntent(p, PlayerEvent.PE_DESTROYED),
				new CloseConnectionIntent(p, DisconnectReason.APPLICATION));
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
				Player oldPlayer = old.player();
				CreatureObject oldObj = old.player().getCreatureObject();
				if (oldObj == null || player.equals(oldPlayer)) {
					disappearIterator.remove();
				}
			}
		}
	}

	private record DisappearPlayer(long time, Player player) {

		@Override
			public boolean equals(Object o) {
				if (o == null)
					return false;
				if (!(o instanceof DisappearPlayer))
					return false;
				return ((DisappearPlayer) o).player().equals(player);
			}

		@Override
			public int hashCode() {
				return player.hashCode();
			}
		}
	
}
