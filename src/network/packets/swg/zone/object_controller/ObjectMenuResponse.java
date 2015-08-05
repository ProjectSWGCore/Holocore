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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import resources.RadialOption;

public class ObjectMenuResponse extends ObjectController {
	
	public static final int CRC = 0x0147;
	
	private long targetId;
	private long requestorId;
	private List <RadialOption> options;
	private int counter;
	
	public ObjectMenuResponse(long objectId) {
		super(objectId, CRC);
	}
	
	public ObjectMenuResponse(long objectId, long targetId, long requestorId, List<RadialOption> options, int counter) {
		super(objectId, CRC);
		this.targetId = targetId;
		this.requestorId = requestorId;
		this.options = options;
		this.counter = counter;
		setTargetId(targetId);
		setRequestorId(requestorId);
		setRadialOptions(options);
		setCounter(counter);
	}
	
	public ObjectMenuResponse(ByteBuffer data) {
		super(CRC);
		options = new ArrayList<>();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		targetId = getLong(data);
		requestorId = getLong(data);
		int optionCount = getInt(data);
		for (int i = 0; i < optionCount; i++) {
			RadialOption option = new RadialOption();
			getByte(data); // option number
			option.setParentId(getByte(data));
			option.setId(getShort(data));
			option.setOptionType(getByte(data));
			option.setText(getUnicode(data));
			options.add(option);
		}
		counter = getByte(data);
	}
	
	public ByteBuffer encode() {
		int optionsDataSize = 0;
		for (RadialOption o : options) {
			optionsDataSize += 9;
			if (o.getText() != null || !o.getText().isEmpty())
				optionsDataSize += o.getText().length()*2;
			else
				optionsDataSize += 4;
		}
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + optionsDataSize + 21);
		encodeHeader(data);
		
		addLong(data, targetId);
		addLong(data, requestorId);

		addInt(data, options.size());
		for (int i = 0; i < options.size(); i++) {
			RadialOption option = options.get(i);
			addByte(data, i + 1);
			addByte(data, option.getParentId());
			addShort(data, option.getId());
			addByte(data, option.getOptionType());
			if (option.getText() != null || !option.getText().isEmpty())
				addUnicode(data, option.getText());
			else
				data.putInt(0);
		}
		addByte(data, counter);
		return data;
	}

	public void setTargetId(long targetId) {
		this.targetId = targetId;
	}
	
	public void setRequestorId(long requestorId) {
		this.requestorId = requestorId;
	}
	
	public void setRadialOptions(List<RadialOption> options) {
		this.options = options;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public long getTargetId() {
		return targetId;
	}
	
	public long getRequestorId() {
		return requestorId;
	}
	
	public List<RadialOption> getOptions() {
		return options;
	}
	
	public int getCounter() {
		return counter;
	}
	
}
