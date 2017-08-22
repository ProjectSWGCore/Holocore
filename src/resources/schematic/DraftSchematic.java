package resources.schematic;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.data.CRC;

import resources.schematic.IngridientSlot.IngridientType;

public class DraftSchematic {

	private final List<IngridientSlot> ingridientSlot; // needed for both

	private int itemsPerContainer; // not needed for Datapadschematic
	private String craftedSharedTemplate; // needed for Datapadschematic
	private int complexity; // needed for Datapadschematic
	private long combinedCrc; // needed for Datapadschematic
	private int volume; // needed for Datapadschematic
	private boolean canManufacture; // not needed for Datapadschematic

	public DraftSchematic() {
		this.ingridientSlot = new ArrayList<>();
		this.itemsPerContainer = 0;
		this.craftedSharedTemplate = "";
		this.complexity = 0;
		this.combinedCrc = 0;
		this.volume = 0;
		this.canManufacture = false;
		createSchematic();
	}

	public int getItemsPerContainer() {
		return itemsPerContainer;
	}

	public void setItemsPerContainer(int itemsPerContainer) {
		this.itemsPerContainer = itemsPerContainer;
	}

	public String getCraftedSharedTemplate() {
		return craftedSharedTemplate;
	}

	public void setCraftedSharedTemplate(String craftedSharedTemplate) {
		this.craftedSharedTemplate = craftedSharedTemplate;
	}

	public int getComplexity() {
		return complexity;
	}

	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}

	public long getCombinedCrc() {
		return combinedCrc;
	}

	public void setCombinedCrc(long combinedCrc) {
		this.combinedCrc = combinedCrc;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public boolean isCanManufacture() {
		return canManufacture;
	}

	public void setCanManufacture(boolean canManufacture) {
		this.canManufacture = canManufacture;
	}

	public List<IngridientSlot> getIngridientSlot() {
		return ingridientSlot;
	}

	// hardcoded for testing
	private void createSchematic() {
		String serverTemplate = "object/draft_schematic/food/component/shared_container_small_glass.iff";
		String clientTemplate = "object/tangible/component/food/shared_container_small_glass.iff";

		CRC serverCrc = new CRC(serverTemplate);
		CRC clientCrc = new CRC(clientTemplate);

		combinedCrc = (((long) serverCrc.getCrc() << 32) & 0xFFFFFFFF00000000l)	| (clientCrc.getCrc() & 0x00000000FFFFFFFFl);
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