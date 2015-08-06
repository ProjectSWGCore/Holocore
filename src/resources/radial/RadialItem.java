package resources.radial;

import java.util.Hashtable;
import java.util.Map;

public enum RadialItem {
	UNKNOWN							(0x0000, 1, ""),
	COMBAT_TARGET					(0x0001, 1, ""),
	COMBAT_UNTARGET					(0x0002, 1, ""),
	COMBAT_ATTACK					(0x0003, 1, ""),
	COMBAT_PEACE					(0x0004, 1, ""),
	COMBAT_DUEL						(0x0005, 1, ""),
	COMBAT_DEATH_BLOW				(0x0006, 1, ""),
	EXAMINE							(0x0007, 1, ""),
	EXAMINE_CHARACTERSHEET			(0x0008, 1, ""),
	TRADE_START						(0x0009, 1, ""),
	TRADE_ACCEPT					(0x000A, 1, ""),
	ITEM_PICKUP						(0x000B, 1, ""),
	ITEM_EQUIP						(0x000C, 1, ""),
	ITEM_UNEQUIP					(0x000D, 1, ""),
	ITEM_DROP						(0x000E, 1, ""),
	ITEM_DESTROY					(0x000F, 1, ""),
	ITEM_TOKEN						(0x0010, 1, ""),
	ITEM_OPEN						(0x0011, 1, ""),
	ITEM_OPEN_NEW_WINDOW			(0x0012, 1, ""),
	ITEM_ACTIVATE					(0x0013, 1, ""),
	ITEM_DEACTIVATE					(0x0014, 1, ""),
	ITEM_USE						(0x0015, 1, ""),
	ITEM_USE_SELF					(0x0016, 1, ""),
	ITEM_USE_OTHER					(0x0017, 1, ""),
	ITEM_SIT						(0x0018, 1, ""),
	ITEM_MAIL						(0x0019, 1, ""),
	CONVERSE_START					(0x001A, 1, ""),
	CONVERSE_RESPOND				(0x001B, 1, ""),
	CONVERSE_RESPONSE				(0x001C, 1, ""),
	CONVERSE_STOP					(0x001D, 1, ""),
	CRAFT_OPTIONS					(0x001E, 1, ""),
	CRAFT_START						(0x001F, 1, ""),
	CRAFT_HOPPER_INPUT				(0x0020, 1, ""),
	CRAFT_HOPPER_OUTPUT				(0x0021, 1, ""),
	TERMINAL_MISSION_LIST			(0x0022, 1, ""),
	MISSION_DETAILS					(0x0023, 1, ""),
	LOOT							(0x0024, 1, ""),
	LOOT_ALL						(0x0025, 1, ""),
	GROUP_INVITE					(0x0026, 1, ""),
	GROUP_JOIN						(0x0027, 1, ""),
	GROUP_LEAVE						(0x0028, 1, ""),
	GROUP_KICK						(0x0029, 1, ""),
	GROUP_DISBAND					(0x002A, 1, ""),
	GROUP_DECLINE					(0x002B, 1, ""),
	EXTRACT_OBJECT					(0x002C, 1, ""),
	PET_CALL						(0x002D, 1, ""),
	TERMINAL_AUCTION_USE			(0x002E, 1, ""),
	CREATURE_FOLLOW					(0x002F, 1, ""),
	CREATURE_STOP_FOLLOW			(0x0030, 1, ""),
	SPLIT							(0x0031, 1, ""),
	IMAGEDESIGN						(0x0032, 1, ""),
	SET_NAME						(0x0033, 1, ""),
	ITEM_ROTATE						(0x0034, 1, ""),
	ITEM_ROTATE_RIGHT				(0x0035, 1, ""),
	ITEM_ROTATE_LEFT				(0x0036, 1, ""),
	ITEM_MOVE						(0x0037, 1, ""),
	ITEM_MOVE_FORWARD				(0x0038, 1, ""),
	ITEM_MOVE_BACK					(0x0039, 1, ""),
	ITEM_MOVE_UP					(0x003A, 1, ""),
	ITEM_MOVE_DOWN					(0x003B, 1, ""),
	PET_STORE						(0x003C, 1, ""),
	VEHICLE_GENERATE				(0x003D, 1, ""),
	VEHICLE_STORE					(0x003E, 1, ""),
	MISSION_ABORT					(0x003F, 1, ""),
	MISSION_END_DUTY				(0x0040, 1, ""),
	SHIP_MANAGE_COMPONENTS			(0x0041, 1, ""),
	WAYPOINT_AUTOPILOT				(0x0042, 1, ""),
	PROGRAM_DROID					(0x0043, 1, ""),
	VEHICLE_OFFER_RIDE				(0x0044, 1, ""),
	ITEM_PUBLIC_CONTAINER_USE1		(0x0045, 1, ""),
	COLLECTIONS						(0x0046, 1, ""),
	GROUP_MASTER_LOOTER				(0x0047, 1, ""),
	GROUP_MAKE_LEADER				(0x0048, 1, ""),
	GROUP_LOOT						(0x0049, 1, ""),
	ITEM_ROTATE_FORWARD				(0x004A, 1, ""),
	ITEM_ROTATE_BACKWARD			(0x004B, 1, ""),
	ITEM_ROTATE_CLOCKWISE			(0x004C, 1, ""),
	ITEM_ROTATE_COUNTERCLOCKWISE	(0x004D, 1, ""),
	ITEM_ROTATE_RANDOM				(0x004E, 1, ""),
	ITEM_ROTATE_RANDOM_YAW			(0x004F, 1, ""),
	ITEM_ROTATE_RANDOM_PITCH		(0x0050, 1, ""),
	ITEM_ROTATE_RANDOM_ROLL			(0x0051, 1, ""),
	ITEM_ROTATE_RESET				(0x0052, 1, ""),
	ITEM_ROTATE_COPY				(0x0053, 1, ""),
	ITEM_MOVE_COPY_LOCATION			(0x0054, 1, ""),
	ITEM_MOVE_COPY_HEIGHT			(0x0055, 1, ""),
	GROUP_TELL						(0x0056, 1, ""),
	ITEM_WP_SETCOLOR				(0x0057, 1, ""),
	ITEM_WP_SETCOLOR_BLUE			(0x0058, 1, ""),
	ITEM_WP_SETCOLOR_GREEN			(0x0059, 1, ""),
	ITEM_WP_SETCOLOR_ORANGE			(0x005A, 1, ""),
	ITEM_WP_SETCOLOR_YELLOW			(0x005B, 1, ""),
	ITEM_WP_SETCOLOR_PURPLE			(0x005C, 1, ""),
	ITEM_WP_SETCOLOR_WHITE			(0x005D, 1, ""),
	ITEM_MOVE_LEFT					(0x005E, 1, ""),
	ITEM_MOVE_RIGHT					(0x005F, 1, ""),
	ROTATE_APPLY					(0x0060, 1, ""),
	ROTATE_RESET					(0x0061, 1, ""),
	WINDOW_LOCK						(0x0062, 1, ""),
	WINDOW_UNLOCK					(0x0063, 1, ""),
	GROUP_CREATE_PICKUP_POINT		(0x0064, 1, ""),
	GROUP_USE_PICKUP_POINT			(0x0065, 1, ""),
	GROUP_USE_PICKUP_POINT_NOCAMP	(0x0066, 1, ""),
	VOICE_SHORTLIST_REMOVE			(0x0067, 1, ""),
	VOICE_INVITE					(0x0068, 1, ""),
	VOICE_KICK						(0x0069, 1, ""),
	ITEM_EQUIP_APPEARANCE			(0x006A, 1, ""),
	ITEM_UNEQUIP_APPEARANCE			(0x006B, 1, ""),
	OPEN_STORYTELLER_RECIPE			(0x006C, 1, ""),
	CLIENT_MENU_LAST				(0x006D, 1, ""),
	SERVER_MENU1					(0x006E, 1, ""),
	SERVER_MENU2					(0x006F, 1, ""),
	BANK_TRANSFER					(0x0070, 3, "@sui:bank_credits"),
	BANK_ITEMS						(0x0071, 3, "@sui:bank_items"),
	SERVER_PET_MOUNT				(0x011F, 1, ""),
	SERVER_VEHICLE_ENTER_EXIT		(0x0124, 1, "");
	
	private static final Map<Integer, RadialItem> INT_TO_ITEM = new Hashtable<>(values().length);
	private int id;
	private int optionType;
	private String text;
	
	static {
		for (RadialItem item : values()) {
			INT_TO_ITEM.put(item.getId(), item);
		}
	}
	
	RadialItem(int id, int optionType, String text) {
		this.id = id;
		this.optionType = optionType;
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
	
}
