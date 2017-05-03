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

package resources.chat;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Waverunner
 */
public enum ChatResult {
	NONE							(-1), // The client will just display an "unknown error code" if this is used.
	SUCCESS							(0),
	TARGET_AVATAR_DOESNT_EXIST		(4),
	ROOM_INVALID_ID					(5),
	ROOM_INVALID_NAME				(6),
	CUSTOM_FAILURE					(9),
	ROOM_AVATAR_BANNED				(12),
	ROOM_PRIVATE					(13),
	ROOM_AVATAR_NO_PERMISSION		(16),
	IGNORED							(23),
	ROOM_ALREADY_EXISTS				(24),
	ROOM_ALREADY_JOINED				(36),
	CHAT_SERVER_UNAVAILABLE			(1000000),
	ROOM_DIFFERENT_FACTION			(1000001),
	ROOM_NOT_GCW_DEFENDER_FACTION	(1000005);
	
	private static final Map<Integer, ChatResult> RESULT_MAP = new HashMap<>();
	
	static {
		for (ChatResult result : values()) {
			RESULT_MAP.put(result.getCode(), result);
		}
	}
	
	private final int code;
	
	ChatResult(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static ChatResult fromInteger(int code) {
		ChatResult result = RESULT_MAP.get(code);
		if (result == null)
			return NONE;
		return result;
	}
}
