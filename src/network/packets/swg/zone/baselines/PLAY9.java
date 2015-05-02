package network.packets.swg.zone.baselines;

import java.nio.ByteBuffer;


public class PLAY9 extends Baseline {
	
	public void decodeBaseline(ByteBuffer data) {
		
	}
	
	public ByteBuffer encodeBaseline() {
		int length = 0x0143;
		ByteBuffer data = ByteBuffer.allocate(length);
		addInt(  data, 0x1F);
		addInt(  data, 0);
		addInt(  data, 0);
		addShort(data, 0);
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 3);
		addByte( data, 0);
		addInt(  data, 0x30254293);
		addInt(  data, 0xD805AB60);
		addInt(  data, 1);
		addByte( data, 0);
		addInt(  data, 0x4BB23CAE);
		addInt(  data, 0x6C750908);
		addInt(  data, 1);
		addByte( data, 0);
		addInt(  data, 0x83AADF10);
		addInt(  data, 0x757A3F17);
		addInt(  data, 1);
		addByte( data, 0);
		addByte( data, 0);
		addShort(data, 0);
		addInt(  data, 0);
		addInt(  data, 8);
		addInt(  data, 0);
		addInt(  data, 10);
		addInt(  data, 0x21);
		addAscii(data, "atima");
		addAscii(data, "brokovo");
		addAscii(data, "daymian");
		addAscii(data, "dow-jones");
		addAscii(data, "eclipse.pandoren");
		addAscii(data, "eclipse.rabivesk");
		addAscii(data, "kenpachie");
		addAscii(data, "melony");
		addAscii(data, "omatchi'");
		addAscii(data, "sobli");
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 1);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 2);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addShort(  data, 0);
		return data;
	}
}
