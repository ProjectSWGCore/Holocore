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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import main.ProjectSWG;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.ServerTimeMessage;
import network.packets.swg.zone.ServerWeatherMessage;
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import resources.Terrain;
import resources.WeatherType;
import resources.control.Intent;
import resources.control.Service;
import resources.player.Player;
import resources.player.PlayerEvent;
import utilities.ThreadUtilities;

public final class EnvironmentService extends Service {
	
	private final long cycleDuration;
	private final Terrain[] terrains;
	private final WeatherType[] weatherTypes;
	private final Map<Terrain, WeatherType> weatherForTerrain;
	private final Random random;

	private ScheduledExecutorService executor;
	
	public EnvironmentService() {
		cycleDuration = 600;	// Ziggy: 10 minutes, 600 seconds
		terrains = Terrain.values();
		weatherForTerrain = new HashMap<>();
		weatherTypes = WeatherType.values();
		random = new Random();
		registerForIntent(PlayerEventIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		executor = Executors.newScheduledThreadPool(2, ThreadUtilities.newThreadFactory("environment-service"));
		for (Terrain t : terrains) {
			weatherForTerrain.put(t, randomWeather());
			executor.scheduleAtFixedRate(new WeatherChanger(t), 0, cycleDuration, TimeUnit.SECONDS);
		}
		executor.scheduleAtFixedRate(() -> { updateTime(); }, 0, 1, TimeUnit.SECONDS);
		
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		return super.start();
	}
	
	@Override
	public boolean stop() {
		try {
			if (executor != null) {
				executor.shutdownNow();
				executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			
		}
		return super.stop();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if(i.getType().equals(PlayerEventIntent.TYPE))
			if (i instanceof PlayerEventIntent) {
				PlayerEventIntent pei = (PlayerEventIntent) i;
				
				if(pei.getEvent().equals(PlayerEvent.PE_ZONE_IN_CLIENT))
					handleZoneIn(pei);
				}
	}
	
	private void handleZoneIn(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		Terrain t = p.getCreatureObject().getTerrain();
		
		p.sendPacket(constructWeatherPacket(t));
	}
	
	private void updateTime() {
		ServerTimeMessage stm = new ServerTimeMessage((long) (ProjectSWG.getCoreTime()/1E3));
		new NotifyPlayersPacketIntent(stm).broadcast();
	}
	
	private void setWeather(Terrain terrain, WeatherType type) {
		SWGPacket swm;
		
		// Ziggy: Prevent packets containing the same weather from being sent
		if(weatherForTerrain.containsKey(terrain))
			if(type.equals(weatherForTerrain.get(terrain)))
				return;
		
		weatherForTerrain.put(terrain, type);
		
		swm = constructWeatherPacket(terrain);
		
		new NotifyPlayersPacketIntent(swm, terrain).broadcast();
	}
	
	private SWGPacket constructWeatherPacket(Terrain terrain) {
		ServerWeatherMessage swm = new ServerWeatherMessage();
		WeatherType type = weatherForTerrain.get(terrain);
		
		swm.setType(type);
		swm.setCloudVectorX(random.nextFloat());	// randomised
		swm.setCloudVectorZ(random.nextFloat());	// randomised	
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
			
			setWeather(terrain, randomWeather());
		}
		
	}
	
}
