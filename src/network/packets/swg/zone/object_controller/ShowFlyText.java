package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

import resources.common.RGB;
import resources.encodables.OutOfBandPackage;
import resources.encodables.StringId;
import resources.network.NetBuffer;

public class ShowFlyText extends ObjectController {
	
	public static final int CRC = 0x01BD;
	
	private StringId text;
	private OutOfBandPackage oob;
	private double scale;
	private RGB rgb;
	private Set<Flag> display;
	
	public ShowFlyText(long objectId, String text, Scale scale, RGB rgb, Flag ... flags) {
		this(objectId, new StringId(text), scale, rgb, flags);
	}
	
	public ShowFlyText(long objectId, StringId text, Scale scale, RGB rgb, Flag ... flags) {
		this(objectId, text, new OutOfBandPackage(), scale, rgb, flags);
	}
	
	public ShowFlyText(long objectId, OutOfBandPackage oob, Scale scale, RGB rgb, Flag ... flags) {
		this(objectId, new StringId(), oob, scale, rgb, flags);
	}
	
	public ShowFlyText(long objectId, StringId text, OutOfBandPackage oob, Scale scale, RGB rgb, Flag ... flags) {
		this(objectId, text, oob, scale.getSize(), rgb, flags);
	}
	
	public ShowFlyText(long objectId, StringId text, OutOfBandPackage oob, double scale, RGB rgb, Flag ... flags) {
		super(objectId, CRC);
		this.text = text;
		this.oob = oob;
		this.scale = scale;
		this.rgb = rgb;
		this.display = EnumSet.noneOf(Flag.class);
		setDisplayFlag(flags);
	}
	
	public ShowFlyText(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		getLong(data);
		text = getEncodable(data, StringId.class);
		oob = getEncodable(data, OutOfBandPackage.class);
		scale = getFloat(data);
		rgb = getEncodable(data, RGB.class);
		display = Flag.getFlyFlags(getInt(data));
	}
	
	@Override
	public ByteBuffer encode() {
		byte [] oobRaw = oob.encode();
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 27 + text.getFile().length() + text.getKey().length() + oobRaw.length);
		encodeHeader(data.getBuffer());
		data.addLong(getObjectId());
		data.addEncodable(text);
		data.addRawArray(oobRaw);
		data.addFloat((float) scale);
		data.addEncodable(rgb);
		data.addInt(getDisplayBitmask());
		return data.getBuffer();
	}
	
	public StringId getText() {
		return text;
	}
	
	public void setText(StringId text) {
		this.text = text;
	}
	
	public OutOfBandPackage getOob() {
		return oob;
	}
	
	public void setOob(OutOfBandPackage oob) {
		this.oob = oob;
	}
	
	public double getScale() {
		return scale;
	}
	
	public void setScale(double scale) {
		this.scale = scale;
	}
	
	public RGB getRgb() {
		return rgb;
	}
	
	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}
	
	public Set<ShowFlyText.Flag> getDisplayFlags() {
		return display;
	}
	
	public void setDisplayFlag(ShowFlyText.Flag ... flags) {
		for (Flag f : flags)
			display.add(f);
	}
	
	public void removeDisplayFlag(ShowFlyText.Flag ... flags) {
		for (Flag f : flags)
			display.remove(f);
	}
	
	private int getDisplayBitmask() {
		int bitmask = 0;
		for (Flag f : display)
			bitmask |= f.getMask();
		return bitmask;
	}
	
	public enum Scale {
		SMALLEST(1.0),
		SMALL	(1.2),
		MEDIUM	(1.5),
		LARGE	(2.0),
		LARGEST	(2.5);
		
		private double size;
		
		Scale(double size) {
			this.size = size;
		}
		
		public double getSize() {
			return size;
		}
	}
	
	public enum Flag {
		PRIVATE					(0x0001),
		SHOW_IN_CHAT_BOX		(0x0002),
		IS_DAMAGE_FROM_PLAYER	(0x0004),
		IS_SNARE				(0x0008),
		IS_GLANCING_BLOW		(0x0010),
		IS_CRITICAL_HIT			(0x0020),
		IS_LUCKY				(0x0040),
		IS_DOT					(0x0080),
		IS_BLEED				(0x0100),
		IS_HEAL					(0x0200),
		IS_FREESHOT				(0x0400);
		
		private static final Flag [] VALUES = values();
		
		private int num;
		
		Flag(int num) {
			this.num = num;
		}
		
		public int getMask() {
			return num;
		}
		
		public static Set<Flag> getFlyFlags(int num) {
			Set<Flag> flags = EnumSet.noneOf(Flag.class);
			for (Flag flag : VALUES) {
				if ((num & flag.getMask()) != 0)
					flags.add(flag);
			}
			return flags;
		}
	}
}

