package network.packets.swg.zone.auction;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class CommoditiesItemTypeListResponse extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("CommoditiesItemTypeListResponse");
	
	private String serverName;
	private int subCategoryCounter;
	private int subCatagory;
	private int itemsInSubCategory;
	private String categoryName;
	private int placeholder;
	private String type;	

	public CommoditiesItemTypeListResponse(String serverName, int subCategoryCounter, int subCatagory, int itemsInSubCategory, String categoryName, int placeholder, String type) {
		this.serverName = serverName;
		this.subCategoryCounter = subCategoryCounter;
		this.subCatagory = subCatagory;
		this.itemsInSubCategory = itemsInSubCategory;
		this.categoryName = categoryName;
		this.placeholder = placeholder;
		this.type = type;
	}

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;		
		serverName = data.getAscii();	
		subCategoryCounter = data.getInt();
		subCatagory = data.getInt();
		itemsInSubCategory = data.getInt();
		categoryName = data.getAscii();
		placeholder = data.getInt();
		type = data.getUnicode();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(30 + serverName.length() + categoryName.length() + type.length()*2);
		data.addShort(8);
		data.addInt(CRC);
		data.addAscii(serverName);
		data.addInt(subCategoryCounter);
		data.addInt(subCatagory);
		data.addInt(itemsInSubCategory);
		data.addAscii(categoryName);
		data.addInt(placeholder);
		data.addUnicode(type);
		return data;
	}

	public String getServerName() {
		return serverName;
	}

	public int getSubCategoryCounter() {
		return subCategoryCounter;
	}

	public int getSubCatagory() {
		return subCatagory;
	}

	public int getItemsInSubCategory() {
		return itemsInSubCategory;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public int getPlaceholder() {
		return placeholder;
	}

	public String getType() {
		return type;
	}
}