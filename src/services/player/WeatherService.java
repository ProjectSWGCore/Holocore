package services.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.ServerWeatherMessage;
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import resources.Terrain;
import resources.WeatherType;
import resources.control.Intent;
import resources.control.Service;
import resources.player.Player;
import resources.player.PlayerEvent;

public final class WeatherService extends Service {
	
	private final long cycleDuration;
	private final Terrain[] terrains;
	private final ScheduledExecutorService executor;
	private final WeatherType[] weatherTypes;
	private final Map<Terrain, WeatherType> weatherForTerrain;
	private final Random random;
	
	public WeatherService() {
		cycleDuration = 900;	// Ziggy: 15 minutes, 900 seconds
		terrains = Terrain.values();
		executor = Executors.newScheduledThreadPool(terrains.length);
		weatherForTerrain = new HashMap<>();
		weatherTypes = WeatherType.values();
		random = new Random();
	}
	
	@Override
	public boolean initialize() {
		for(Terrain t : terrains) {
			weatherForTerrain.put(t, randomWeather());
			
			executor.scheduleAtFixedRate(
					new WeatherChanger(t), 0, 
					cycleDuration, TimeUnit.SECONDS);
		}
		
		registerForIntent(PlayerEventIntent.TYPE);
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if(i.getType().equals(PlayerEventIntent.TYPE))
			if (i instanceof PlayerEventIntent) {
				PlayerEventIntent pei = (PlayerEventIntent) i;
				
				if(pei.getEvent().equals(PlayerEvent.PE_ZONE_IN))
					handleZoneIn(pei);
				}
	}
	
	private final void handleZoneIn(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		Terrain t = p.getCreatureObject().getLocation().getTerrain();
		
		p.sendPacket(constructWeatherPacket(t));
	}
	
	private final void setWeather(Terrain terrain, WeatherType type) {
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
		swm.setCloudVectorZ(0);	// Ziggy: Always 0, clouds don't move up/down
		swm.setCloudVectorY(random.nextFloat());	// randomised
		
		return swm;
	}
	
	private WeatherType randomWeather() {
		return weatherTypes[random.nextInt(weatherTypes.length)];
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
