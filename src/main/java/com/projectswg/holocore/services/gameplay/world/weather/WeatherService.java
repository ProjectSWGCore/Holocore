/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
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
package com.projectswg.holocore.services.gameplay.world.weather;

import com.projectswg.common.data.WeatherType;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.ServerTimeMessage;
import com.projectswg.common.network.packets.swg.zone.ServerWeatherMessage;
import com.projectswg.common.utilities.ThreadUtilities;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.zone.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class WeatherService extends Service {
	
	private final Duration cycleDuration;
	private final Terrain[] terrains;
	private final WeatherType[] weatherTypes;
	private final Map<Terrain, WeatherType> weatherForTerrain;
	private final Random random;
	private final ScheduledExecutorService executor;
	
	public WeatherService() {
		cycleDuration = Duration.of(10, ChronoUnit.MINUTES);
		terrains = Terrain.values();
		weatherForTerrain = new EnumMap<>(Terrain.class);
		weatherTypes = WeatherType.values();
		random = new Random();
		executor = Executors.newScheduledThreadPool(2, ThreadUtilities.newThreadFactory("environment-service"));
	}
	
	@Override
	public boolean initialize() {
		for (Terrain t : terrains) {
			weatherForTerrain.put(t, randomWeather());
			executor.scheduleAtFixedRate(new WeatherChanger(t), 0, cycleDuration.toSeconds(), TimeUnit.SECONDS);
		}
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		executor.scheduleAtFixedRate(this::updateTime, 30, 30, TimeUnit.SECONDS);
		return super.start();
	}
	
	@Override
	public boolean terminate() {
		boolean success = true;
		try {
			executor.shutdownNow();
			success = executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
		}
		return super.terminate() && success;
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei){
		if(pei.getEvent().equals(PlayerEvent.PE_ZONE_IN_CLIENT))
			handleZoneIn(pei);
	}

	private void handleZoneIn(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		Terrain t = p.getCreatureObject().getTerrain();
		
		p.sendPacket(constructWeatherPacket(t));
	}
	
	private void updateTime() {
		new NotifyPlayersPacketIntent(new ServerTimeMessage(ProjectSWG.getGalacticTime())).broadcast();
	}
	
	private void updateWeather(Terrain terrain, WeatherType type) {
		boolean weatherTypeAlreadyActive = weatherForTerrain.containsKey(terrain) && type.equals(weatherForTerrain.get(terrain));
		if(weatherTypeAlreadyActive)
			return;
		
		weatherForTerrain.put(terrain, type);

		ServerWeatherMessage serverWeatherMessage = constructWeatherPacket(terrain);
		new NotifyPlayersPacketIntent(serverWeatherMessage, terrain).broadcast();
	}
	
	private ServerWeatherMessage constructWeatherPacket(Terrain terrain) {
		ServerWeatherMessage swm = new ServerWeatherMessage();
		WeatherType type = weatherForTerrain.get(terrain);
		
		swm.setType(type);
		swm.setCloudVectorX(random.nextFloat()+1);	// randomised
		swm.setCloudVectorZ(random.nextFloat()+1);	// randomised	
		swm.setCloudVectorY(0);	// Ziggy: Always 0, clouds don't move up/down
		
		return swm;
	}
	
	private WeatherType randomWeather() {
		WeatherType weather = WeatherType.CLEAR;
		float roll = random.nextFloat();
		
		for(WeatherType candidate : weatherTypes)
			if(roll <= candidate.getChance())
				weather = candidate;
		
		return weather;
	}
	
	private class WeatherChanger implements Runnable {

		private final Terrain terrain;
		
		private WeatherChanger(Terrain terrain) {
			this.terrain = terrain;
		}
		
		@Override
		public void run() {
			if(!random.nextBoolean()) // 50/50 chance of weather change
				return;
			
			updateWeather(terrain, randomWeather());
		}
		
	}
	
}
