package resources.schematic;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.data.CRC;

import resources.schematic.IngridientSlot.IngridientType;

public class DraftSchematic {

	private String parent;
	private int itemsPerContainer;
	private String craftedSharedTemplate;
	private final List<IngridientSlot> ingridientSlot;
	private int complexity;
	private long combinedCrc;
	private int volume;
	private boolean canManufacture;

	public DraftSchematic() {
		this.parent = "";
		this.itemsPerContainer = 0;
		this.craftedSharedTemplate = "";
		this.ingridientSlot = new ArrayList<>();
		this.complexity = 0;
		this.combinedCrc = 0;
		this.volume = 0;
		this.canManufacture = false;
		createSchematic();
	}

	public String getParent() {
		return parent;
	}

	public int getItemsPerContainer() {
		return itemsPerContainer;
	}

	public String getCraftedSharedTemplate() {
		return craftedSharedTemplate;
	}

	public List<IngridientSlot> getIngridientSlot() {
		return ingridientSlot;
	}

	public int getComplexity() {
		return complexity;
	}

	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public void setItemsPerContainer(int itemsPerContainer) {
		this.itemsPerContainer = itemsPerContainer;
	}

	public void setCraftedSharedTemplate(String craftedSharedTemplate) {
		this.craftedSharedTemplate = craftedSharedTemplate;
	}

	public long getCombinedCrc() {
		return combinedCrc;
	}

	public int getVolume() {
		return volume;
	}

	public boolean isCanManufacture() {
		return canManufacture;
	}	
	
	//hardcoded for testing
	private void createSchematic() {
		String serverTemplate = "object/draft_schematic/food/component/shared_container_small_glass.iff";
		String clientTemplate = "object/tangible/component/food/shared_container_small_glass.iff";
		
		CRC serverCrc = new CRC(serverTemplate);
		CRC clientCrc = new CRC(clientTemplate);
		
		combinedCrc = (((long) serverCrc.getCrc() << 32) & 0xFFFFFFFF00000000l) | (clientCrc.getCrc() & 0x00000000FFFFFFFFl);
		parent = "object/draft_schematic/food/base_food_schematic.iff";
		volume = 1;
		itemsPerContainer = 25;
		complexity = 5;
		canManufacture = true;
		craftedSharedTemplate = clientTemplate;
		IngridientSlot slot = new IngridientSlot("craft_food_ingredients_n.crystal", false);
		slot.addSlotDataOption(new DraftSlotDataOption("craft_food_ingredients_n", "crystal", IngridientType.IT_RESOURCE_CLASS, 10));
		ingridientSlot.add(slot);		
	}
}