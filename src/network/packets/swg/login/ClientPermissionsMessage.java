package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientPermissionsMessage extends SWGPacket {
	
	public static final int CRC = 0xE00730E5;
	
	private boolean canLogin;
	private boolean canCreateRegularCharacter;
	private boolean canCreateJediCharacter;
	private boolean canSkipTutorial;
	
	public ClientPermissionsMessage() {
		canLogin = true;
		canCreateRegularCharacter = true;
		canCreateJediCharacter = true;
		canSkipTutorial = true;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		canLogin = getBoolean(data);
		canCreateRegularCharacter = getBoolean(data);
		canCreateJediCharacter = getBoolean(data);
		canSkipTutorial = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(  data, 5);
		addInt(    data, CRC);
		addBoolean(data, canLogin);
		addBoolean(data, canCreateRegularCharacter);
		addBoolean(data, canCreateJediCharacter);
		addBoolean(data, canSkipTutorial);
		return data;
	}
	
	public void setCanLogin(boolean can) { this.canLogin = can; }
	public void setCanCreateRegularCharacter(boolean can) { this.canCreateRegularCharacter = can; }
	public void setCanCreateJediCharacter(boolean can) { this.canCreateJediCharacter = can; }
	public void setCanSkipTutorial(boolean can) { this.canSkipTutorial = can; }
	
	public boolean canLogin() { return canLogin; }
	public boolean canCreateRegularCharacter() { return canCreateRegularCharacter; }
	public boolean canCreateJediCharacter() { return canCreateJediCharacter; }
	public boolean canSkipTutorial() { return canSkipTutorial; }
}
