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

package resources.collections;

import network.packets.swg.zone.baselines.Baseline;
import resources.encodables.Encodable;
import resources.network.DeltaBuilder;
import resources.objects.SWGObject;
import resources.player.PlayerState;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;


/**
 * @author Waverunner
 */
public class SWGBitSet extends BitSet implements Encodable, Serializable {
	private static final long serialVersionUID = 200L;
	
	private final Baseline.BaselineType baseline;
	private int view;
	private int updateType;

	/**
	 * Creates a new {@link SWGBitSet} for the defined baseline with the given view and update.
	 * Note that this is an extension of {@link BitSet}
	 * @param baseline {@link Baseline.BaselineType} for this BitSet, should be the same as the parent class this list resides in
	 * @param view The baseline number this BitSet resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that this BitSet resides at within the baseline
	 */
	public SWGBitSet(Baseline.BaselineType baseline, int view, int updateType) {
		super(128); // Seems to be the default size for the bitmask sets in packets
		this.baseline = baseline;
		this.view = view;
		this.updateType = updateType;
	}

	@Override
	public byte[] encode() {
		int[] list = toList();
		ByteBuffer buffer = ByteBuffer.allocate(4 + (list.length * 4)).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(list.length);
		for (int bits : list) {
			buffer.putInt(bits);
		}

		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		// TODO: Decode method for SWGBitSet
		throw new NotImplementedException();
	}

	public void sendDeltaMessage(SWGObject target) {
		if (target.getOwner() == null || target.getOwner().getPlayerState() != PlayerState.ZONED_IN) {
			return;
		}

		DeltaBuilder builder = new DeltaBuilder(target, baseline, view, updateType, encode());
		builder.send();
	}

	public int[] toList() {
		// TODO: This is working somehow, but position is wrong...
		int[] integers = new int[4];
		int count = 0;
		int position = 0; // Position within the bit set

		for (int i = 0; i < size(); i++) {
			if (position >= 32)
				count++;

			int setBitIndex = nextSetBit(i);
			if (setBitIndex == -1) // No more bits after this one
				break;

			integers[count] |= (0x00000001 << setBitIndex);
			i += setBitIndex;
		}

		return integers;
	}
}
