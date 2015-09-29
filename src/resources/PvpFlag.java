package resources;

import java.util.EnumSet;

public enum PvpFlag {
	
	ATTACKABLE(1<<0),
	AGGRESSIVE(1<<1),
	OVERT(1<<2),
	TEF(1<<3),
	PLAYER(1<<4),
	ENEMY(1<<5),
	GOING_OVERT(1<<6),
	GOING_COVERT(1<<7),
	DUEL(1<<8);
	
	private int bitmask;
	
	PvpFlag(int bitmask) {
		this.bitmask = bitmask;
	}
	
	public int getBitmask() {
		return bitmask;
	}
	public static EnumSet<PvpFlag> getFlags(int bits) {
		EnumSet <PvpFlag> states = EnumSet.noneOf(PvpFlag.class);
		for (PvpFlag state : values()) {
			if ((state.getBitmask() & bits) != 0)
				states.add(state);
		}
		return states;
	}
	
}