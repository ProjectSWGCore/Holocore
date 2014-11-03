package network.packets.swg.login;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import network.packets.swg.SWGPacket;

public class OfflineServersMessage extends SWGPacket {
	
	public static final int CRC = 0xF41A5265;
	
	private List <String> offlineServers;
	
	public OfflineServersMessage() {
		offlineServers = new ArrayList<String>();
	}
	
	public OfflineServersMessage(List <String> offline) {
		this.offlineServers = offline;
	}
	
	public OfflineServersMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int listCount = getInt(data);
		offlineServers = new ArrayList<String>(listCount);
		for (int i = 0 ; i < listCount; i++)
			offlineServers.add(getAscii(data));
	}
	
	public ByteBuffer encode() {
		int strLength = 0;
		for (String str : offlineServers)
			strLength += 2 + str.length();
		ByteBuffer data = ByteBuffer.allocate(10 + strLength);
		addShort(data, 2);
		addInt(  data, CRC);
		for (String str : offlineServers)
			addAscii(data, str);
		return data;
	}
	
	public List <String> getOfflineServers() {
		return offlineServers;
	}
	
	public void setOfflineServers(List <String> offline) {
		offlineServers = offline;
	}
	
	public void addOflineServer(String offline) {
		offlineServers.add(offline);
	}
	
}
