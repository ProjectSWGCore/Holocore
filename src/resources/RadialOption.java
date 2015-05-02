package resources;

public class RadialOption {
	private short id;
	private byte parentId;
	private byte optionType; // 1 unless the option text isn't listed in the radial item list (datatable). - ANHWiki
	private String text;
	
	public RadialOption() { }
	public RadialOption(String text, short id, byte parentId, byte optionType) {
		this.text = text;
		this.id = id;
		this.parentId = parentId;
		this.optionType = (text == null || text.isEmpty()) ? (byte) 1 : optionType;
	}
	
	public short getId() { return id; }
	public void setId(short id) { this.id = id; }
	public byte getParentId() { return parentId; }
	public void setParentId(byte parentId) { this.parentId = parentId; }
	public void setOptionType(byte optionType) { this.optionType = optionType; }
	public void setText(String text) { this.text = text; }
	public byte getOptionType() { return optionType; }
	public String getText() { return text; }
	@Override
	public String toString() { 
		return String.valueOf(id) + ": parentId- " + String.valueOf(parentId) + " optionType:" + String.valueOf(optionType) + " text-" + text; 
	}
}