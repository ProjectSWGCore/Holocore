package resources.combat;

import java.util.EnumSet;
import java.util.Set;

public enum DamageType {
	KINETIC					(1),
	ENERGY					(2),
	BLAST					(4),
	STUN					(8),
	RESTAINT				(16),
	ELEMENTAL_HEAT			(32),
	ELEMENTAL_COLD			(64),
	ELEMENTAL_ACID			(128),
	ELEMENTAL_ELECTRICAL	(256);
	
	private static final DamageType [] VALUES = values();
	
	private int num;
	
	DamageType(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static DamageType getDamageType(int num) {
		for (DamageType type : VALUES) {
			if ((num & type.getNum()) != 0)
				return type;
		}
		return KINETIC;
	}
	
	public static Set<DamageType> getDamageTypes(int num) {
		Set<DamageType> types = EnumSet.noneOf(DamageType.class);
		for (DamageType type : VALUES) {
			if ((num & type.getNum()) != 0)
				types.add(type);
		}
		return types;
	}
}
