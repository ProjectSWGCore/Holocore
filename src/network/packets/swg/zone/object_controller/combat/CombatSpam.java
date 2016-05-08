package network.packets.swg.zone.object_controller.combat;

import java.nio.ByteBuffer;

import network.packets.swg.zone.object_controller.ObjectController;
import resources.Point3D;
import resources.combat.AttackInfo;
import resources.combat.DamageType;
import resources.combat.HitLocation;
import resources.encodables.StringId;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class CombatSpam extends ObjectController {
	
	public static final int CRC = 0x0134;
	
	private byte dataType;
	private long attacker;
	private Point3D attackerPosition;
	private long defender;
	private Point3D defenderPosition;
	private long weapon;
	private StringId weaponName;
	private StringId attackName;
	private AttackInfo info;
	private String spamMessage;
	private int spamType;
	
	public CombatSpam(long objectId) {
		super(objectId, CRC);
	}
	
	public CombatSpam(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		info = new AttackInfo();
		dataType = getByte(data);
		attacker = getLong(data);
		attackerPosition = getEncodable(data, Point3D.class);
		defender = getLong(data);
		defenderPosition = getEncodable(data, Point3D.class);
		if (isAttackDataWeaponObject(dataType) || isAttackWeaponName(dataType)) {
			if (isAttackDataWeaponObject(dataType))
				weapon = getLong(data);
			else
				weaponName = getEncodable(data, StringId.class);
			attackName = getEncodable(data, StringId.class);
			info.setSuccess(getBoolean(data));
			if (info.isSuccess()) {
				info.setArmor(getLong(data));
				info.setRawDamage(getInt(data));
				info.setDamageType(DamageType.getDamageType(getInt(data)));
				info.setElementalDamage(getInt(data));
				info.setElementalDamageType(DamageType.getDamageType(getInt(data)));
				info.setBleedDamage(getInt(data));
				info.setCriticalDamage(getInt(data));
				info.setBlockedDamage(getInt(data));
				info.setFinalDamage(getInt(data));
				info.setHitLocation(HitLocation.getHitLocation(getInt(data)));
				info.setCrushing(getBoolean(data));
				info.setStrikethrough(getBoolean(data));
				info.setStrikethroughAmount(getFloat(data));
				info.setEvadeResult(getBoolean(data));
				info.setEvadeAmount(getFloat(data));
				info.setBlockResult(getBoolean(data));
				info.setBlock(getInt(data));
			} else {
				info.setDodge(getBoolean(data));
				info.setParry(getBoolean(data));
			}
		} else {
			// spamMessage = getUnicode(data);
			getInt(data);
		}
		info.setCritical(getBoolean(data));
		info.setGlancing(getBoolean(data));
		info.setProc(getBoolean(data));
		spamType = getInt(data);
		if (isMessageData(dataType)) {
			// Extra stuff?
		}
	}
	
	@Override
	public ByteBuffer encode() {
		NetBuffer data = NetBuffer.allocate(getEncodeSize());
		data.addByte(dataType);
		data.addLong(attacker);
		data.addEncodable(attackerPosition);
		data.addLong(defender);
		data.addEncodable(defenderPosition);
		if (isAttackDataWeaponObject(dataType) || isAttackWeaponName(dataType)) {
			if (isAttackDataWeaponObject(dataType))
				data.addLong(weapon);
			else
				data.addEncodable(weaponName);
			data.addEncodable(attackName);
			data.addBoolean(info.isSuccess());
			if (info.isSuccess()) {
				data.addLong(info.getArmor());
				data.addInt(info.getRawDamage());
				data.addInt(info.getDamageType().getNum());
				data.addInt(info.getElementalDamage());
				data.addInt(info.getElementalDamageType().getNum());
				data.addInt(info.getBleedDamage());;
				data.addInt(info.getCriticalDamage());
				data.addInt(info.getBlockedDamage());
				data.addInt(info.getFinalDamage());
				data.addInt(info.getHitLocation().getNum());
				data.addBoolean(info.isCrushing());
				data.addBoolean(info.isStrikethrough());
				data.addFloat((float) info.getStrikethroughAmount());
				data.addBoolean(info.isEvadeResult());
				data.addFloat((float) info.getEvadeAmount());
				data.addBoolean(info.isBlockResult());
				data.addInt(info.getBlock());
			} else {
				data.addBoolean(info.isDodge());
				data.addBoolean(info.isParry());
			}
		} else {
			data.addInt(0); // ihnfk
		}
		data.addBoolean(info.isCritical());
		data.addBoolean(info.isGlancing());
		data.addBoolean(info.isProc());
		data.addInt(spamType);
		return data.getBuffer();
	}
	
	private int getEncodeSize() {
		int size = 48;
		if (isAttackDataWeaponObject(dataType))
			size += 9 + getStringIdSize(attackName) + (info.isSuccess() ? 97 : 2);
		else if (isAttackWeaponName(dataType))
			size += 1 + getStringIdSize(attackName) + getStringIdSize(weaponName) + (info.isSuccess() ? 97 : 2);
		else
			size += 4; // I have no idea what's in this struct
		return size;
	}
	
	private int getStringIdSize(StringId sid) {
		return 8 + sid.getFile().length() + sid.getKey().length();
	}
	
	/**
	 * Sets the various fields based off of this creature object
	 * 
	 * @param attacker the attacker
	 */
	public void setAttacker(CreatureObject attacker) {
		setDataType((byte) 0);
		setAttacker(attacker.getObjectId());
		setAttackerPosition(attacker.getLocation().getPosition());
		if (attacker.getEquippedWeapon() != null)
			setWeapon(attacker.getEquippedWeapon().getObjectId());
		else
			setWeapon(0);
	}
	
	/**
	 * Sets the various fields based off of this creature object
	 * 
	 * @param defender the defender
	 */
	public void setDefender(SWGObject defender) {
		setDefender(defender.getObjectId());
		setDefenderPosition(defender.getLocation().getPosition());
	}
	
	public byte getDataType() {
		return dataType;
	}
	
	public long getAttacker() {
		return attacker;
	}
	
	public Point3D getAttackerPosition() {
		return attackerPosition;
	}
	
	public long getDefender() {
		return defender;
	}
	
	public Point3D getDefenderPosition() {
		return defenderPosition;
	}
	
	public long getWeapon() {
		return weapon;
	}
	
	public StringId getWeaponName() {
		return weaponName;
	}
	
	public StringId getAttackName() {
		return attackName;
	}
	
	public AttackInfo getInfo() {
		return info;
	}
	
	public String getSpamMessage() {
		return spamMessage;
	}
	
	public int getSpamType() {
		return spamType;
	}
	
	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}
	
	public void setAttacker(long attacker) {
		this.attacker = attacker;
	}
	
	public void setAttackerPosition(Point3D attackerPosition) {
		this.attackerPosition = attackerPosition;
	}
	
	public void setDefender(long defender) {
		this.defender = defender;
	}
	
	public void setDefenderPosition(Point3D defenderPosition) {
		this.defenderPosition = defenderPosition;
	}
	
	public void setWeapon(long weapon) {
		this.weapon = weapon;
	}
	
	public void setWeaponName(StringId weaponName) {
		this.weaponName = weaponName;
	}
	
	public void setAttackName(StringId attackName) {
		this.attackName = attackName;
	}
	
	public void setInfo(AttackInfo info) {
		this.info = info;
	}
	
	public void setSpamMessage(String spamMessage) {
		this.spamMessage = spamMessage;
	}
	
	public void setSpamType(int spamType) {
		this.spamType = spamType;
	}
	
	private boolean isAttackDataWeaponObject(byte b) {
		return b == 0;
	}
	
	private boolean isAttackWeaponName(byte b) {
		return b == 1;
	}
	
	private boolean isMessageData(byte b) {
		return b == 2;
	}
	
}

