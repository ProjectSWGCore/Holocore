package resources.radial;

import java.util.Hashtable;
import java.util.Map;

public enum RadialItem {
	UNKNOWN							(""),
	COMBAT_TARGET					(""),
	COMBAT_UNTARGET					(""),
	COMBAT_ATTACK					(""),
	COMBAT_PEACE					(""),
	COMBAT_DUEL						(""),
	COMBAT_DEATH_BLOW				(""),
	EXAMINE							(""),
	EXAMINE_CHARACTERSHEET			(""),
	TRADE_START						(""),
	TRADE_ACCEPT					(""),
	ITEM_PICKUP						(""),
	ITEM_EQUIP						(""),
	ITEM_UNEQUIP					(""),
	ITEM_DROP						(""),
	ITEM_DESTROY					(""),
	ITEM_TOKEN						(""),
	ITEM_OPEN						(""),
	ITEM_OPEN_NEW_WINDOW			(""),
	ITEM_ACTIVATE					(""),
	ITEM_DEACTIVATE					(""),
	ITEM_USE						(""),
	ITEM_USE_SELF					(""),
	ITEM_USE_OTHER					(""),
	ITEM_SIT						(""),
	ITEM_MAIL						(""),
	CONVERSE_START					(""),
	CONVERSE_RESPOND				(""),
	CONVERSE_RESPONSE				(""),
	CONVERSE_STOP					(""),
	CRAFT_OPTIONS					(""),
	CRAFT_START						(""),
	CRAFT_HOPPER_INPUT				(""),
	CRAFT_HOPPER_OUTPUT				(""),
	TERMINAL_MISSION_LIST			(""),
	MISSION_DETAILS					(""),
	LOOT							(""),
	LOOT_ALL						(""),
	GROUP_INVITE					(""),
	GROUP_JOIN						(""),
	GROUP_LEAVE						(""),
	GROUP_KICK						(""),
	GROUP_DISBAND					(""),
	GROUP_DECLINE					(""),
	EXTRACT_OBJECT					(""),
	PET_CALL						(""),
	TERMINAL_AUCTION_USE			(""),
	CREATURE_FOLLOW					(""),
	CREATURE_STOP_FOLLOW			(""),
	SPLIT							(""),
	IMAGEDESIGN						(""),
	SET_NAME						(""),
	ITEM_ROTATE						(""),
	ITEM_ROTATE_RIGHT				(""),
	ITEM_ROTATE_LEFT				(""),
	ITEM_MOVE						(""),
	ITEM_MOVE_FORWARD				(""),
	ITEM_MOVE_BACK					(""),
	ITEM_MOVE_UP					(""),
	ITEM_MOVE_DOWN					(""),
	PET_STORE						(""),
	VEHICLE_GENERATE				(""),
	VEHICLE_STORE					(""),
	MISSION_ABORT					(""),
	MISSION_END_DUTY				(""),
	SHIP_MANAGE_COMPONENTS			(""),
	WAYPOINT_AUTOPILOT				(""),
	PROGRAM_DROID					(""),
	VEHICLE_OFFER_RIDE				(""),
	ITEM_PUBLIC_CONTAINER_USE1		(""),
	COLLECTIONS						(""),
	GROUP_MASTER_LOOTER				(""),
	GROUP_MAKE_LEADER				(""),
	GROUP_LOOT						(""),
	ITEM_ROTATE_FORWARD				(""),
	ITEM_ROTATE_BACKWARD			(""),
	ITEM_ROTATE_CLOCKWISE			(""),
	ITEM_ROTATE_COUNTERCLOCKWISE	(""),
	ITEM_ROTATE_RANDOM				(""),
	ITEM_ROTATE_RANDOM_YAW			(""),
	ITEM_ROTATE_RANDOM_PITCH		(""),
	ITEM_ROTATE_RANDOM_ROLL			(""),
	ITEM_ROTATE_RESET				(""),
	ITEM_ROTATE_COPY				(""),
	ITEM_MOVE_COPY_LOCATION			(""),
	ITEM_MOVE_COPY_HEIGHT			(""),
	GROUP_TELL						(""),
	ITEM_WP_SETCOLOR				(""),
	ITEM_WP_SETCOLOR_BLUE			(""),
	ITEM_WP_SETCOLOR_GREEN			(""),
	ITEM_WP_SETCOLOR_ORANGE			(""),
	ITEM_WP_SETCOLOR_YELLOW			(""),
	ITEM_WP_SETCOLOR_PURPLE			(""),
	ITEM_WP_SETCOLOR_WHITE			(""),
	ITEM_MOVE_LEFT					(""),
	ITEM_MOVE_RIGHT					(""),
	ROTATE_APPLY					(""),
	ROTATE_RESET					(""),
	WINDOW_LOCK						(""),
	WINDOW_UNLOCK					(""),
	GROUP_CREATE_PICKUP_POINT		(""),
	GROUP_USE_PICKUP_POINT			(""),
	GROUP_USE_PICKUP_POINT_NOCAMP	(""),
	VOICE_SHORTLIST_REMOVE			(""),
	VOICE_INVITE					(""),
	VOICE_KICK						(""),
	ITEM_EQUIP_APPEARANCE			(""),
	ITEM_UNEQUIP_APPEARANCE			(""),
	OPEN_STORYTELLER_RECIPE			(""),
	CLIENT_MENU_LAST				(""),
	SERVER_MENU1					(""),
	SERVER_MENU2					(""),
	SERVER_PET_MOUNT				(""),
	SERVER_VEHICLE_ENTER_EXIT		(""),
	BANK_TRANSFER					("@sui:bank_credits"),
	BANK_ITEMS						("@sui:bank_items"),
	BANK_WITHDRAW_ALL				("@sui:bank_withdrawall"),
	BANK_DEPOSIT_ALL				("@sui:bank_depositall"),
	BANK_RESERVE					("@sui:bank_galactic_reserve"),
	BANK_RESERVE_WITHDRAW			("@sui:bank_galactic_reserve_withdraw"),
	BANK_RESERVE_DEPOSIT			("@sui:bank_galactic_reserve_deposit");
	
	private static final Map<Integer, RadialItem> INT_TO_ITEM = new Hashtable<>(values().length);
	private int id;
	private int optionType;
	private String text;
	
	static {
		for (RadialItem item : values()) {
			INT_TO_ITEM.put(item.getId(), item);
		}
	}
	
	RadialItem(String text) {
		this.id = RadialItemInit.getNextItemId();
		this.optionType = 3;
		this.text = text;
	}
	
	public int getId() {
		return id;
	}
	
	public int getOptionType() {
		return optionType;
	}
	
	public String getText() {
		return text;
	}
	
	/**
	 * Gets the RadialItem from the selection id. If the item is undefined,
	 * then NULL is returned
	 * @param id the selection id that maps to a RadialItem
	 * @return the RadialItem represented by the selection, or NULL if it does
	 * not exist
	 */
	public static RadialItem getFromId(int id) {
		return INT_TO_ITEM.get(id);
	}
	
}
