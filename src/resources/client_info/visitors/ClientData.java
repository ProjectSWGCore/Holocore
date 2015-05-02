package resources.client_info.visitors;

import java.nio.ByteBuffer;

public abstract class ClientData {

	public abstract void handleData(String node, ByteBuffer data, int size);
}
