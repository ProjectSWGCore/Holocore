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

import java.io.File;
import java.io.IOException;

/**
 * Created by Waverunner on 6/9/2015
 */
public abstract class DataFactory {

	protected ClientData readFile(String filename) {
		if (filename == null || filename.isEmpty()) {
			System.err.println("File cannot be null or empty!");
			return null;
		}
		File file = new File(getFolder() + filename);
		if (!file.isFile()) {
			System.err.println("Not a file: " + file);
			return null;
		}

		SWGFile swgFile = new SWGFile();

		try {
			swgFile.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ClientData clientData = createDataObject(swgFile.getType());
		if (clientData == null)
			return null;

		clientData.readIff(swgFile);
		return clientData;
	}

	protected File writeFile(SWGFile swgFile, ClientData data) {
		if (swgFile == null || data == null) {
			System.err.println("File or data objects cannot be null or empty!");
			return null;
		}

		File save = new File(swgFile.getFileName());
		data.writeIff(swgFile);
		try {
			swgFile.save(save);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return save;
	}

	protected abstract ClientData createDataObject(String type);
	protected ClientData createDataObject(SWGFile swgFile) {
		return createDataObject(swgFile.getType());
	}

	protected abstract String getFolder();
}
