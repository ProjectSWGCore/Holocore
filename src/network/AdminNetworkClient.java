package network;

import com.projectswg.common.network.packets.SWGPacket;

import java.nio.channels.SocketChannel;

public class AdminNetworkClient extends NetworkClient {
	
	public AdminNetworkClient(SocketChannel socket) {
		super(socket);
	}
	
	@Override
	protected boolean allowInbound(SWGPacket packet) {
		return true;
	}
	
	@Override
	protected boolean allowOutbound(SWGPacket packet) {
		return true;
	}
}
