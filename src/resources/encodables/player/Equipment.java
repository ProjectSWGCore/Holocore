package resources.encodables.player;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.common.CRC;
import resources.network.BaselineBuilder.Encodable;
import resources.objects.weapon.WeaponObject;

public class Equipment implements Encodable {

	private WeaponObject 	weapon;
	private byte[] 			customizationString;
	private int 			arrangementId = 4;
	private long 			objectId;
	private int				templateCRC;
	
	public Equipment(long objectId, String template) {
		this.objectId = objectId;
		this.templateCRC = CRC.getCrc(template);
	}
	
	public Equipment(WeaponObject weapon) {
		this.objectId = weapon.getObjectId();
		this.templateCRC = weapon.getCrc();
		this.weapon = weapon;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer;
		byte[] weaponData = null;
		
		if (weapon != null) {
			weaponData = weapon.encode();
			
			buffer = ByteBuffer.allocate(19 + weaponData.length).order(ByteOrder.LITTLE_ENDIAN);
		} else {
			buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
		}

		if (customizationString == null) buffer.putShort((short) 0);
		else buffer.put(customizationString);
		
		buffer.putInt(arrangementId);
		buffer.putLong(objectId);
		buffer.putInt(templateCRC);
		
		if (weapon != null) {
			buffer.put((byte) 0x01);
			buffer.put(weaponData);
		} else {
			buffer.put((byte) 0x00);
		}
		
		return buffer.array();
	}

	public byte[] getCustomizationString() {return customizationString;}
	public void setCustomizationString(byte[] customizationString) { this.customizationString = customizationString; }

	public int getArrangementId() { return arrangementId; }
	public void setArrangementId(int arrangementId) { this.arrangementId = arrangementId; }

	public long getObjectId() { return objectId; }
	public void setObjectId(long objectId) { this.objectId = objectId; }

	public int getTemplateCRC() { return templateCRC; }
	public void setTemplateCRC(int templateCRC) { this.templateCRC = templateCRC; }
}
