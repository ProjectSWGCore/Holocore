package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class CreateCharacterFailure extends SWGPacket {
	
	public static final int CRC = 0xDF333C6E;
	private NameFailureReason reason;
	
	public CreateCharacterFailure() {
		reason = NameFailureReason.NAME_RETRY;
	}
	
	public CreateCharacterFailure(NameFailureReason reason) {
		this.reason = reason;
	}
	
	public void decode(ByteBuffer data) {
		
	}
	
	public ByteBuffer encode() {
		String errorString = nameFailureTranslation(reason);
		ByteBuffer data = ByteBuffer.allocate(20 + errorString.length());
		addShort(  data, 3);
		addInt(    data, CRC);
		addUnicode(data, "");
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, errorString);
		return data;
	}
	
	private String nameFailureTranslation(NameFailureReason reason) {
		switch (reason) {
			case NAME_DECLINED_EMPTY:
				return "name_declined_empty";
			case NAME_IN_USE:
				return "name_declined_in_use";
			case NAME_RETRY:
				return "name_declined_retry";
			case NAME_SYNTAX:
				return "name_declined_syntax";
			case NAME_TOO_FAST:
				return "name_declined_too_fast";
		}
		return "name_declined_retry";
	}
	
	public enum NameFailureReason {
		NAME_DECLINED_EMPTY,
		NAME_TOO_FAST,
		NAME_RETRY,
		NAME_SYNTAX,
		NAME_IN_USE
	}
}
