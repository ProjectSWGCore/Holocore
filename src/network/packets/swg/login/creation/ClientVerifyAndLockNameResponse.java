package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientVerifyAndLockNameResponse extends SWGPacket {
	
	public static final int CRC = 0x9B2C6BA7;
	
	private String name = "";
	private ErrorMessage error = ErrorMessage.NAME_APPROVED;
	
	public ClientVerifyAndLockNameResponse() {
		
	}
	
	public ClientVerifyAndLockNameResponse(String name, ErrorMessage error) {
		this.name = name;
		this.error = error;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		name = getUnicode(data);
		error = ErrorMessage.valueOf(getAscii(data).toUpperCase());
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(20 + error.name().length() + name.length() * 2);
		addShort(  data, 9);
		addInt(    data, CRC);
		addUnicode(data, name);
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, error.name().toLowerCase());
		return data;
	}
	
	public enum ErrorMessage {
		NAME_APPROVED,
		NAME_APPROVED_MODIFIED,
		NAME_DECLINED_SYNTAX,
		NAME_DECLINED_EMPTY,
		NAME_DECLINED_RACIALLY_INAPPROPRIATE,
		NAME_DECLINED_FICTIONALLY_INAPPROPRIATE,
		NAME_DECLINED_PROFANE,
		NAME_DECLINED_IN_USE,
		NAME_DECLINED_RESERVED,
		NAME_DECLINED_NO_TEMPLATE,
		NAME_DECLINED_NOT_CREATURE_TEMPLATE,
		NAME_DECLINED_NO_NAME_GENERATOR,
		NAME_DECLINED_CANT_CREATE_AVATAR,
		NAME_DECLINED_INTERNAL_ERROR,
		NAME_DECLINED_RETRY,
		NAME_DECLINED_TOO_FAST,
		NAME_DECLINED_NOT_AUTHORIZED_FOR_SPECIES;
	}
	
}
