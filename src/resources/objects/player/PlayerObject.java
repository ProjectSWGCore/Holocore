package resources.objects.player;

import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.baselines.PLAY6;
import resources.network.BaselineBuilder;
import resources.objects.creature.CreatureObject;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;

public class PlayerObject extends IntangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private String	profession		= "";
	
	private String	biography		= "";
	private boolean	showBackpack	= true;
	private boolean	showHelmet		= true;
	
	public PlayerObject(long objectId) {
		super(objectId);
	}
	
	public String getProfession() {
		return profession;
	}
	
	public String getBiography() {
		return biography;
	}
	
	public boolean isShowBackpack() {
		return showBackpack;
	}
	
	public boolean isShowHelmet() {
		return showHelmet;
	}
	
	public void setProfession(String profession) {
		this.profession = profession;
	}
	
	public void setBiography(String biography) {
		this.biography = biography;
	}
	
	public void setShowBackpack(boolean showBackpack) {
		this.showBackpack = showBackpack;
	}
	
	public void setShowHelmet(boolean showHelmet) {
		this.showHelmet = showHelmet;
	}
	
	private int getProfessionIcon() {
		switch (profession) {
			case "entertainer_1a":
				return 5;
			case "medic_1a":
				return 10;
			case "officer_1a":
				return 15;
			case "bounty_hunter_1a":
				return 20;
			case "smuggler_1a":
				return 25;
			case "commando_1a":
				return 30;
			case "spy_1a":
				return 35;
			case "force_sensitive_1a":
				return 40;
			case "trader_0a":
			case "trader_0b":
			case "trader_0c":
			case "trader_0d":
			default:
				return 0;
		}
	}
	
	public void createObject(Player target) {
		super.sendSceneCreateObject(target);
		
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.PLAY, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.PLAY, 6);
//		createBaseline6(target, bb);
//		bb.sendTo(target);
		PLAY6 play6 = new PLAY6();
		play6.setId(getObjectId()); play6.setType(BaselineType.PLAY); play6.setNum(6);
		target.sendPacket(new Baseline(getObjectId(), play6));
//		bb = new BaselineBuilder(this, BaselineType.PLAY, 8);
//		createBaseline8(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.PLAY, 9);
//		createBaseline9(target, bb);
//		bb.sendTo(target);
		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	public void createChildrenObjects(Player target) {
		if (getParent() != null && getParent() instanceof CreatureObject)
			target.sendPacket(new UpdatePostureMessage(((CreatureObject)getParent()).getPosture().getId(), getObjectId()));
		super.createChildrenObjects(target);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(0);
		bb.addInt(4); // Flags List Size
		for (int i = 0; i < 4; i++) // 4 flags
			bb.addInt(0);
		bb.addInt(4); // My Profile List Size
		for (int i = 0; i < 4; i++) // 4 flags
			bb.addInt(0);
		bb.addAscii(""); // Title
		bb.addInt(0); // Born Date
		bb.addInt(0); // Total Play Time
		bb.addInt(getProfessionIcon()); // Profession Icon
		bb.addAscii(profession);
		bb.addInt(0); // GCW Points
		bb.addInt(0); // PvP Kills
		bb.addLong(0); // Lifetime GCW Points
		bb.addInt(0); // Lifetime Pvp Kills
		bb.addInt(0); // Collections
			bb.addInt(0);
		bb.addInt(0); // Guild Ranks
			bb.addInt(0);
		bb.addBoolean(showHelmet); // Show Helmet
		bb.addBoolean(showBackpack); // Show Backpack
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addByte(0); // Admin Tag
		bb.addInt(0); // Current Rank
		bb.addFloat(0); // Rank Progress
		bb.addInt(0); // Highest Rebel Rank
		bb.addInt(0); // Highest Imperial Rank
		bb.addInt(0); // Next Update Time
		bb.addAscii(""); // Home
		bb.addByte(0); // Citizenship
		bb.addAscii(""); // City Region Defender 'region'
		bb.addByte(0); // City Region Defender byte #1
		bb.addByte(0); // City Region Defender byte #2
		bb.addAscii(""); // Guild Region Defender 'region'
		bb.addByte(0); // Guild Region Defender byte #1
		bb.addByte(0); // Guild Region Defender byte #2
		bb.addLong(0); // General?
		bb.addInt(0); // Guild Rank Title?
		bb.addShort(0); // Citizen Rank Title? 6 bytes
		bb.addInt(1); // Speeder Elevation
		bb.addAscii(""); // Vehicle Attack Command
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
		bb.addInt(0); // XP List Size
		bb.addInt(0); // Waypoint List Size
		bb.addInt(100); // Current Force Power
		bb.addInt(100); // Max Force Power
		bb.addInt(0); // Current FS Quest List
		bb.addInt(0); // Completed FS Quest List
		bb.addInt(0); // Active Quest
		bb.addInt(0); // Quest Journal
		bb.addAscii(""); // Profession Wheel Position
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
		bb.addInt(0); // Experimentation Flag
		bb.addInt(0); // Crafting Stage
		bb.addLong(0); // Nearest Crafting Station
		bb.addInt(0); // Draft Schematic List
		bb.addInt(0); // List of some kind?
		bb.addInt(0); // Experimentation Points
		bb.addInt(0); // Accomplishment Counter
		bb.addInt(0); // Friends List
		bb.addInt(0); // Ignore List
		bb.addInt(0); // Language ID
		bb.addInt(0); // Current Stomach
		bb.addInt(0); // Max Stomach
		bb.addInt(0); // Current Drink
		bb.addInt(0); // Max Drink
		bb.addInt(0); // Current Consumable
		bb.addInt(0); // Max Consumable
		bb.addInt(0); // Waypoint List
		bb.addInt(0); // Defenders List
		bb.addInt(0); // Kill Meter Points
		bb.addInt(0); // Unk
		bb.addLong(0); // Pet
		bb.addInt(0); // Pet Abilities
		bb.addInt(0); // Active Pet Abilities
		bb.addByte(0); // Unk  sometimes 0x01 or 0x02
		bb.addInt(0); // Unk  sometimes 4
		bb.addLong(0); // Unk  Bitmask starts with 0x20 ends with 0x40
		bb.addLong(0); // Unk
		bb.addByte(0); // Unk
		bb.addLong(0); // Unk  sometimes 856
		bb.addLong(0); // Unk  sometimes 8559
		bb.addInt(0); // Residence Time
	}
	
}
