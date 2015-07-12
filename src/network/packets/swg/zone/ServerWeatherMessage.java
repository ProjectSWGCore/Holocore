package network.packets.swg.zone;

import network.packets.swg.SWGPacket;
import resources.WeatherType;

import java.nio.ByteBuffer;

public class ServerWeatherMessage extends SWGPacket {
	public static final int CRC = getCrc("ServerWeatherMessage");
	
	private WeatherType type;
	private float cloudVectorX;
	private float cloudVectorZ;
	private float cloudVectorY;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		WeatherType type = WeatherType.CLEAR;
		
		switch(getInt(data)) {
			case 0:
				type = WeatherType.CLEAR;
				break;
			case 1:
				type = WeatherType.LIGHT;
				break;
			case 2:
				type = WeatherType.MEDIUM;
				break;
			case 3:
				type = WeatherType.HEAVY;
				break;
		}
		
		this.type = type;
		
		cloudVectorX = getFloat(data);
		cloudVectorZ = getFloat(data);
		cloudVectorY = getFloat(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(22);
		
		addShort(data, 3);
		addInt(data, CRC);
		addInt(data, type.getValue());
		
		addFloat(data, cloudVectorX);
		addFloat(data, cloudVectorZ);
		addFloat(data, cloudVectorY);
		
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
