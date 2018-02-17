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

package com.projectswg.holocore.services.map;

public class MapCategory {
	private String name;
	private int index;
	private boolean isCategory;
	private boolean isSubCategory;
	private boolean canBeActive;
	private String faction;
	private boolean factionVisibleOnly;

	public MapCategory() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public boolean isCategory() {
		return isCategory;
	}

	public void setIsCategory(boolean isCategory) {
		this.isCategory = isCategory;
	}

	public boolean isSubCategory() {
		return isSubCategory;
	}

	public void setIsSubCategory(boolean isSubCategory) {
		this.isSubCategory = isSubCategory;
	}

	public boolean isCanBeActive() {
		return canBeActive;
	}

	public void setCanBeActive(boolean canBeActive) {
		this.canBeActive = canBeActive;
	}

	public String getFaction() {
		return faction;
	}

	public void setFaction(String faction) {
		this.faction = faction;
	}

	public boolean isFactionVisibleOnly() {
		return factionVisibleOnly;
	}

	public void setFactionVisibleOnly(boolean factionVisibleOnly) {
		this.factionVisibleOnly = factionVisibleOnly;
	}
}
