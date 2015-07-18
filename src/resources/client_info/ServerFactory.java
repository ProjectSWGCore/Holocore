/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.client_info;

import resources.client_info.visitors.DatatableData;


/**
 * Created by Waverunner on 6/9/2015
 */
public final class ServerFactory extends DataFactory {
	private static ServerFactory instance;

	public static DatatableData getDatatable(String file) {
		ClientData data = getInstance().readFile(file);
		// Safe type conversion as ServerFactory can only create DatatableData ClientData objects
		return (data != null ? (DatatableData) data : null);
	}

	@Override
	protected ClientData createDataObject(String type) {
		switch(type) {
			case "DTII": return new DatatableData();
			default: return null;
		}
	}

	@Override
	protected String getFolder() {
		return "./serverdata/";
	}

	private static ServerFactory getInstance() {
		if (instance == null)
			instance = new ServerFactory();
		return instance;
	}
}
