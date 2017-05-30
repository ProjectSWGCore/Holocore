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
package services.crafting.resource;

import java.util.concurrent.TimeUnit;

import com.projectswg.common.concurrency.PswgBasicScheduledThread;
import com.projectswg.common.control.Service;

import services.crafting.resource.galactic.GalacticResourceSpawner;
import services.crafting.resource.galactic.storage.GalacticResourceContainer;
import services.crafting.resource.raw.RawResource;
import services.crafting.resource.raw.RawResourceContainer;

/**
 * In charge of spawning, despawning, and overall management of resources
 */
public class ResourceService extends Service {
	
	private final RawResourceContainer container;
	private final GalacticResourceSpawner spawner;
	private final PswgBasicScheduledThread spawnerUpdater;
	
	public ResourceService() {
		this.container = new RawResourceContainer();
		this.spawner = new GalacticResourceSpawner();
		this.spawnerUpdater = new PswgBasicScheduledThread("resource-spawn-updater", () -> spawner.updateAllResources());
	}
	
	@Override
	public boolean initialize() {
		container.loadResources();
		for (RawResource rawResource : container.getResources()) {
			GalacticResourceContainer.getContainer().addRawResource(rawResource);
		}
		spawner.initialize();
		spawnerUpdater.startWithFixedRate(0, TimeUnit.HOURS.toMillis(3));
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		spawnerUpdater.stop();
		spawner.terminate();
		return super.terminate();
	}
	
}
