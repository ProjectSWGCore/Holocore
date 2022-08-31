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
package com.projectswg.holocore.resources.support.objects.swg.cell;

import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CellObject extends SWGObject {
	
	private final Set<Portal> portals;
	
	private boolean	isPublic	= true;
	private int		number		= 0;
	private String	label		= "";
	private String	name		= "";

	private double labelX       = 0;
	private double labelZ       = 0;

	public CellObject(long objectId) {
		super(objectId, BaselineType.SCLT);
		this.portals = new HashSet<>();
	}
	
	public Collection<Portal> getPortals() {
		return Collections.unmodifiableCollection(portals);
	}
	
	public Portal getPortalTo(CellObject neighbor) {
		for (Portal portal : portals) {
			if (portal.getOtherCell(this) == neighbor)
				return portal;
		}
		return null;
	}
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public int getNumber() {
		return number;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getCellName() {
		return name;
	}
	
	public void addPortal(@NotNull Portal portal) {
		portals.add(portal);
	}
	
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setCellName(String name) {
		this.name = name;
	}

	public void setLabelMapPosition(float x, float z) {
		this.labelX = x;
		this.labelZ = z;
	}
	
	@Override
	protected void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addBoolean(isPublic);
		bb.addInt(number);
		bb.incrementOperandCount(2);
	}
	
	@Override
	protected void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addUnicode(label);
		bb.addFloat((float) labelX);
		bb.addFloat(0);
		bb.addFloat((float) labelZ);
		bb.incrementOperandCount(2);
	}
	
	@Override
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		isPublic = buffer.getBoolean();
		number = buffer.getInt();
	}
	
	@Override
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		label = buffer.getUnicode();
		labelX = buffer.getFloat();
		buffer.getFloat();
		labelZ = buffer.getFloat();
	}
	
}
