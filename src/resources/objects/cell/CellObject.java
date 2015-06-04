/***********************************************************************************
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
package resources.objects.cell;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.building.UpdateCellPermissionMessage;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class CellObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private boolean	isPublic	= true;
	private int		number		= 0;
	private String	label		= "";
	
	public CellObject(long objectId) {
		super(objectId, BaselineType.SCLT);
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
	
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	protected void sendBaselines(Player target) {
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.SCLT, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.SCLT, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);
		target.sendPacket(new UpdateCellPermissionMessage((byte) 1, getObjectId()));
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addByte(1);
		bb.addInt(number);
		bb.incrementOperandCount(2);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addLong(0);
		bb.addLong(0);
		bb.incrementOperandCount(2);
	}
	
}
