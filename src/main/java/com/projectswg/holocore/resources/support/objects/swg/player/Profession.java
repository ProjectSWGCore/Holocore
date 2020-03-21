/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.player;

import me.joshlarson.jlcommon.data.EnumLookup;
import org.jetbrains.annotations.NotNull;

public enum Profession {
	UNKNOWN				("",						""),
	BOUNTY_HUNTER		("bounty_hunter_1a",		"bounty_hunter"),
	COMMANDO			("commando_1a",			"commando"),
	ENTERTAINER			("entertainer_1a",		"entertainer"),
	FORCE_SENSITIVE		("force_sensitive_1a",	"force_sensitive"),
	MEDIC				("medic_1a",				"medic"),
	OFFICER				("officer_1a",			"officer"),
	SMUGGLER			("smuggler_1a",			"smuggler"),
	SPY					("spy_1a",				"spy"),
	TRADER_GENERAL		("",						"trader",	"trader"),
	TRADER_DOMESTIC		("trader_0a",			"trader",	"trader_dom"),
	TRADER_STRUCTURES	("trader_0b",			"trader",	"trader_struct"),
	TRADER_MUNITIONS	("trader_0c",			"trader",	"trader_mun"),
	TRADER_ENGINEER		("trader_0d",			"trader",	"trader_eng");
	
	private static final EnumLookup<String, Profession> CLIENT_TO_PROFESSION = new EnumLookup<>(Profession.class, Profession::getClientName);
	private static final EnumLookup<String, Profession> NAME_TO_PROFESSION = new EnumLookup<>(Profession.class, Profession::getName);
	private static final EnumLookup<String, Profession> UNIQUE_TO_PROFESSION = new EnumLookup<>(Profession.class, Profession::getUniqueName);
	
	private final String clientName;
	private final String name;
	private final String unique;
	
	Profession(String clientName, String name) {
		this(clientName, name, name);
	}
	
	Profession(String clientName, String name, String unique) {
		this.clientName = clientName;
		this.name = name;
		this.unique = unique;
	}
	
	/**
	 * Returns a client friendly name for this profession
	 * @return the client name
	 */
	public String getClientName() {
		return clientName;
	}
	
	/**
	 * Returns a trimmed version of the client name, removing the _1a or _0a from the end
	 * @return a human friendly version of the client name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns a unique name for the profession/specialization. This is only different from {@see #getName} for Traders, where it is one of: trader_dom, trader_struct, trader_mun, or trader_eng
	 * @return a unique name for the profession
	 */
	public String getUniqueName() {
		return unique;
	}
	
	/**
	 * Does a lookup for the specific client name and returns the associated Profession, or UNKNOWN if one was not found
	 * @param clientName the client name to look up
	 * @return the matching profession, or UNKNOWN if one was not found   
	 */
	@NotNull
	public static Profession getProfessionFromClient(String clientName) {
		if (clientName.isBlank())
			return UNKNOWN;
		return CLIENT_TO_PROFESSION.getEnum(clientName, Profession.UNKNOWN);
	}
	
	/**
	 * Does a lookup for the name and returns the associated Profession, or UNKNOWN if one was not found
	 * @param name the name to look up
	 * @return the matching profession, or UNKNOWN if one was not found   
	 */
	@NotNull
	public static Profession getProfessionFromName(String name) {
		if ("trader".equals(name))
			return TRADER_GENERAL;
		return NAME_TO_PROFESSION.getEnum(name, Profession.UNKNOWN);
	}
	
	/**
	 * Does a lookup for the unique name and returns the associated Profession, or UNKNOWN if one was not found
	 * @param unique the unique name to look up
	 * @return the matching profession, or UNKNOWN if one was not found   
	 */
	@NotNull
	public static Profession getProfessionFromUnique(String unique) {
		return UNIQUE_TO_PROFESSION.getEnum(unique, Profession.UNKNOWN);
	}
	
}
