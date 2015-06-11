package network.packets.swg.zone;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.WeatherType;
import network.packets.swg.SWGPacket;

public class ServerWeatherMessage extends SWGPacket {

	public static final int CRC = 0x486356EA;
	
	private WeatherType type;
	private float cloudVectorX;
	private float cloudVectorZ;
	private float cloudVectorY;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		WeatherType type = WeatherType.CLEAR;
		
		switch(data.getInt()) {
			case 0:
				type = WeatherType.CLEAR;
				break;
			case 1:
				type = WeatherType.CLOUDY;
				break;
			case 2:
				type = WeatherType.LIGHT;
				break;
			case 3:
				type = WeatherType.MEDIUM;
				break;
			case 4:
				type = WeatherType.HEAVY;
				break;
		}
		
		this.type = type;
		
		cloudVectorX = data.getFloat();
		cloudVectorZ = data.getFloat();
		cloudVectorY = data.getFloat();
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN);
		
		data.putShort((short) 3);
		data.putInt(CRC);
		data.putInt(type.getValue());
		data.putFloat(cloudVectorX);
		data.putFloat(cloudVectorZ);
		data.putFloat(cloudVectorY);
		
		return data;
	}

	public WeatherType getType() {
		return type;
	}

	public void setType(WeatherType type) {
		this.type = type;
	}

	public float getCloudVectorX() {
		return cloudVectorX;
	}

	public void setCloudVectorX(float cloudVectorX) {
		this.cloudVectorX = cloudVectorX;
	}

	public float getCloudVectorZ() {
		return cloudVectorZ;
	}

	public void setCloudVectorZ(float cloudVectorZ) {
		this.cloudVectorZ = cloudVectorZ;
	}

	public float getCloudVectorY() {
		return cloudVectorY;
	}

	public void setCloudVectorY(float cloudVectorY) {
		this.cloudVectorY = cloudVectorY;
	}
	
}
