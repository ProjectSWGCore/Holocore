/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.test.runners;

import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.concurrency.Delay;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.ServiceBase;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public abstract class TestRunnerSynchronousIntents extends TestRunner {
	
	private final Collection<ServiceBase> instantiatedServices = new ArrayList<>();
	private IntentManager intentManager = null;
	
	@Before
	public final void setupSynchronous() {
		intentManager = new IntentManager(1);
		IntentManager.setInstance(intentManager);
	}
	
	@After
	public final void cleanupServices() {
		for (ServiceBase service : instantiatedServices) {
			service.setIntentManager(null);
			service.stop();
			service.terminate();
		}
		intentManager.close();
		IntentManager.setInstance(null);
	}
	
	protected final void registerService(ServiceBase service) {
		service.setIntentManager(Objects.requireNonNull(intentManager));
		service.initialize();
		service.start();
		this.instantiatedServices.add(service);
	}
	
	protected final void registerObject(SWGObject ... objects) {
		for (SWGObject object : objects)
			broadcastAndWait(new ObjectCreatedIntent(object));
	}
	
	protected final void broadcastAndWait(Intent i) {
		i.broadcast();
		while (!i.isComplete()) {
			boolean uninterrupted = Delay.sleepMicro(10);
			assert uninterrupted;
		}
		while (intentManager.getIntentCount() > 0) {
			boolean uninterrupted = Delay.sleepMicro(10);
			assert uninterrupted;
		}
	}
	
}
