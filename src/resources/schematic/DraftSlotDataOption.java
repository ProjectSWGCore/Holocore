package resources.schematic;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

import resources.schematic.IngridientSlot.IngridientType;

public class DraftSlotDataOption implements Encodable {
	
	private String stfName;
	private String ingredientName;
	private IngridientType ingredientType;
	private int amount;
	
	public DraftSlotDataOption(String stfName, String ingredientName, IngridientType ingredientType, int amount) {
		this.stfName = stfName;
		this.ingredientName = ingredientName;
		this.ingredientType = ingredientType;
		this.amount = amount;
	}

	public DraftSlotDataOption(){
		this.stfName = "";
		this.ingredientName = "";
		this.ingredientType = IngridientType.IT_NONE;
		this.amount = 0;
	}
	
	public String getStfName() {
		return stfName;
	}
	
	public String getIngredientName() {
		return ingredientName;
	}
	
	public IngridientType getIngredientType() {
		return ingredientType;
	}
	
	public int getAmount() {
		return amount;
	}
	
	@Override
	public void decode(NetBuffer data) {
		stfName = data.getAscii();
		ingredientName = data.getUnicode();
		ingredientType =  IngridientType.getTypeForInt(data.getInt());
		amount = data.getInt();		
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addAscii(stfName);
		data.addUnicode(ingredientName);
		data.addInt(ingredientType.getId());
		data.addInt(amount);
		return data.array();
	}
	
	@Override
	public int getLength() {
		return 14 + stfName.length() + ingredientName.length() * 2;
	}	
}