/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements;

import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.holocore.resources.gameplay.conversation.requirements.FactionStatusRequirement;
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.RequirementParser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FactionStatusRequirementParser implements RequirementParser<FactionStatusRequirement> {
	
	private static final Map<String, PvpStatus> STATUS_MAP = Map.of(
			"on_leave", PvpStatus.ONLEAVE,
			"combatant", PvpStatus.COMBATANT,
			"special_forces", PvpStatus.SPECIALFORCES
	);
	
	public FactionStatusRequirementParser() {
	
	}
	
	@Override
	public FactionStatusRequirement parse(Map<String, Object> args) {
		Collection<String> statuses = (List<String>) args.get("statuses");
		
		Collection<PvpStatus> allowedPvpStatuses = statuses.stream()
				.map(this::getStatusByName)
				.collect(Collectors.toList());
		
		return new FactionStatusRequirement(allowedPvpStatuses);
	}
	
	private PvpStatus getStatusByName(String status) {
		PvpStatus pvpStatus = STATUS_MAP.get(status);
		
		if (pvpStatus != null) {
			return pvpStatus;
		}
		
		throw new RuntimeException("Unknown status name " + status + ". Valid status names: " + STATUS_MAP.keySet());
	}
}
