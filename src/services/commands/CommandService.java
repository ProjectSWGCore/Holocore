/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.commands;

import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import intents.player.PlayerTransformedIntent;

import network.packets.Packet;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.combat.AttackType;
import resources.combat.DamageType;
import resources.combat.ValidTarget;
import resources.commands.CombatCommand;
import resources.commands.Command;
import resources.commands.ICmdCallback;
import resources.commands.callbacks.*;
import resources.common.CRC;
import resources.objects.SWGObject;
import resources.objects.weapon.WeaponType;
import resources.player.Player;
import resources.server_info.Log;
import services.commands.CommandLauncher.EnqueuedCommand;
import services.galaxy.GalacticManager;

import java.util.List;
import java.util.Locale;
import com.projectswg.common.control.Service;

import resources.combat.DelayAttackEggPosition;
import resources.combat.HitType;
import resources.commands.DefaultPriority;
import resources.objects.creature.CreatureObject;

public class CommandService extends Service {
	
	private final CommandContainer					commandContainer;
	private final CommandLauncher					commandLauncher;
	
	public CommandService() {
		this.commandContainer = new CommandContainer();
		this.commandLauncher = new CommandLauncher();
		
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		registerForIntent(PlayerTransformedIntent.class, pti -> handlePlayerTransformedIntent(pti));
	}
	
	@Override
	public boolean initialize() {
		loadBaseCommands();
		loadCombatCommands();
		registerCallbacks();
		commandLauncher.start();
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		commandLauncher.stop();
		return super.terminate();
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet p = gpi.getPacket();
		if (p instanceof CommandQueueEnqueue) {
			CommandQueueEnqueue controller = (CommandQueueEnqueue) p;
			handleCommandRequest(gpi.getPlayer(), gpi.getGalacticManager(), controller);
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				// No reason to keep their combat queue in the map if they log out
				// This also prevents queued commands from executing after the player logs out
				commandLauncher.removePlayerFromQueue(pei.getPlayer());
				break;
			default:
				break;
		}
	}
	
	private void handlePlayerTransformedIntent(PlayerTransformedIntent pti) {
		CreatureObject creature = pti.getPlayer();
		
		if (creature.isPerforming()) {
			// A performer can transform while dancing...
			return;
		}
		
		commandLauncher.removePlayerFromQueue(creature.getOwner());
	}
	
	private void handleCommandRequest(Player player, GalacticManager galacticManager, CommandQueueEnqueue request) {
		if (!commandContainer.isCommand(request.getCommandCrc())) {
			if (request.getCommandCrc() != 0)
				Log.e("Invalid command crc: %x", request.getCommandCrc());
			return;
		}
		
		Command command = commandContainer.getCommand(request.getCommandCrc());
		// TODO target and target type checks below. Work with Set<TangibleObject> targets from there
		long targetId = request.getTargetId();
		SWGObject target = targetId != 0 ? galacticManager.getObjectManager().getObjectById(targetId) : null;
		
		EnqueuedCommand enqueued = new EnqueuedCommand(command, galacticManager, target, request);
		if (!command.getCooldownGroup().equals("defaultCooldownGroup") && command.isAddToCombatQueue()) {
			commandLauncher.addToQueue(player, enqueued);
		} else {
			// Execute it now
			commandLauncher.doCommand(player, enqueued);
		}
	}
	
	private void loadBaseCommands() {
		// First = Higher Priority, Last = Lower Priority ---- Some tables contain duplicates, ORDER MATTERS!
		String [] commandTables = new String[] {
				"command_table", "command_table_ground", "client_command_table",
				"command_table_space", "client_command_table_ground", "client_command_table_space"
		};
		
		commandContainer.clearCommands();
		for (String table : commandTables) {
			loadBaseCommands(table);
		}
	}
	
	private void loadBaseCommands(String table) {
		DatatableData baseCommands = (DatatableData) ClientFactory.getInfoFromFile("datatables/command/" + table + ".iff");
		
		int godLevel = baseCommands.getColumnFromName("godLevel");
		int cooldownGroup = baseCommands.getColumnFromName("cooldownGroup");
		int cooldownGroup2 = baseCommands.getColumnFromName("cooldownGroup2");
		int cooldownTime = baseCommands.getColumnFromName("cooldownTime");
		int cooldownTime2 = baseCommands.getColumnFromName("cooldownTime2");
		
		for (int row = 0; row < baseCommands.getRowCount(); row++) {
			Object[] cmdRow = baseCommands.getRow(row);
			String commandName = ((String) cmdRow[0]).toLowerCase(Locale.ENGLISH);
			if (commandContainer.isCommand(commandName))
				continue; // skip duplicates - first is higher priority
			
			Command command = new Command(commandName);
			
			command.setCrc(CRC.getCrc(commandName));
			command.setDefaultPriority(DefaultPriority.getDefaultPriority((int) cmdRow[1]));
			command.setScriptHook((String) cmdRow[2]);
			command.setCppHook((String) cmdRow[4]);
			command.setDefaultTime((float) cmdRow[6]);
			command.setCharacterAbility((String) cmdRow[7]);
			command.setCombatCommand(false);
			command.setCooldownGroup((String) cmdRow[cooldownGroup]);
			command.setCooldownGroup2((String) cmdRow[cooldownGroup2]);
			command.setCooldownTime((float) cmdRow[cooldownTime]);
			command.setCooldownTime2((float) cmdRow[cooldownTime2]);
			
			// Ziggy: The amount of columns in the table seems to change for each row
			if (cmdRow.length >= 83) {
				Object addToCombatQueue = cmdRow[82];
				
				// Ziggy: Sometimes this column contains a String... uwot SOE?
				if (addToCombatQueue instanceof Boolean) {
					command.setAddToCombatQueue((Boolean) addToCombatQueue);
				}
			}
			
			if (godLevel >= 0) {
				command.setGodLevel((int) cmdRow[godLevel]);
			}
			
			commandContainer.addCommand(command);
		}
	}
	
	private CombatCommand createAsCombatCommand(Command c) {
		CombatCommand cc = new CombatCommand(c.getName());
		cc.setCrc(c.getCrc());
		cc.setScriptHook(c.getScriptHook());
		cc.setCppHook(c.getScriptHook());
		cc.setDefaultTime(c.getDefaultTime());
		cc.setCharacterAbility(c.getCharacterAbility());
		cc.setGodLevel(c.getGodLevel());
		cc.setCombatCommand(true);
		cc.setCooldownGroup(c.getCooldownGroup());
		cc.setCooldownGroup2(c.getCooldownGroup2());
		cc.setCooldownTime(c.getCooldownTime());
		cc.setCooldownTime2(c.getCooldownTime2());
		cc.setMaxRange(c.getMaxRange());
		return cc;
	}
	
	private void loadCombatCommands() {
		DatatableData combatCommands = (DatatableData) ClientFactory.getInfoFromFile("datatables/combat/combat_data.iff");
		int validTarget = combatCommands.getColumnFromName("validTarget");
		int forceCombat = combatCommands.getColumnFromName("forcesCharacterIntoCombat");
		int attackType = combatCommands.getColumnFromName("attackType");
		int healthCost = combatCommands.getColumnFromName("healthCost");
		int actionCost = combatCommands.getColumnFromName("actionCost");
		int damageType = combatCommands.getColumnFromName("damageType");
		int ignoreDistance = combatCommands.getColumnFromName("ignore_distance");
		int pvpOnly = combatCommands.getColumnFromName("pvp_only");
		int attackRolls = combatCommands.getColumnFromName("attack_rolls");
		int animDefault = combatCommands.getColumnFromName("animDefault");
		int percentAddFromWeapon = combatCommands.getColumnFromName("percentAddFromWeapon");
		int addedDamage = combatCommands.getColumnFromName("addedDamage");
		int buffNameTarget = combatCommands.getColumnFromName("buffNameTarget");
		int buffNameSelf = combatCommands.getColumnFromName("buffNameSelf");
		int maxRange = combatCommands.getColumnFromName("maxRange");
		int hitType = combatCommands.getColumnFromName("hitType");
		int delayAttackEggTemplate = combatCommands.getColumnFromName("delayAttackEggTemplate");
		int delayAttackParticle = combatCommands.getColumnFromName("delayAttackParticle");
		int initialDelayAttackInterval = combatCommands.getColumnFromName("initialDelayAttackInterval");
		int delayAttackInterval = combatCommands.getColumnFromName("delayAttackInterval");
		int delayAttackLoops = combatCommands.getColumnFromName("delayAttackLoops");
		int delayAttackEggPosition = combatCommands.getColumnFromName("delayAttackEggPosition");
		int coneLength = combatCommands.getColumnFromName("coneLength");
		// animDefault anim_unarmed anim_onehandmelee anim_twohandmelee anim_polearm
		// anim_pistol anim_lightRifle anim_carbine anim_rifle anim_heavyweapon
		// anim_thrown anim_onehandlightsaber anim_twohandlightsaber anim_polearmlightsaber
		for (int row = 0; row < combatCommands.getRowCount(); row++) {
			Object[] cmdRow = combatCommands.getRow(row);
			
			Command c = commandContainer.getCommand(CRC.getCrc(((String) cmdRow[0]).toLowerCase(Locale.ENGLISH)));
			if (c == null)
				continue;
			CombatCommand cc = createAsCombatCommand(c);
			commandContainer.removeCommand(c);
			cc.setValidTarget(ValidTarget.getValidTarget((Integer) cmdRow[validTarget]));
			cc.setForceCombat(((int) cmdRow[forceCombat]) != 0);
			cc.setAttackType(AttackType.getAttackType((Integer) cmdRow[attackType]));
			cc.setHealthCost((float) cmdRow[healthCost]);
			cc.setActionCost((float) cmdRow[actionCost]);
			cc.setDamageType(DamageType.getDamageType((Integer) cmdRow[damageType]));
			cc.setIgnoreDistance(((int) cmdRow[ignoreDistance]) != 0);
			cc.setPvpOnly(((int) cmdRow[pvpOnly]) != 0);
			cc.setAttackRolls((int) cmdRow[attackRolls]);
			cc.setDefaultAnimation(getAnimationList((String) cmdRow[animDefault]));
			cc.setPercentAddFromWeapon((float) cmdRow[percentAddFromWeapon]);
			cc.setAddedDamage((int) cmdRow[addedDamage]);
			cc.setBuffNameTarget((String) cmdRow[buffNameTarget]);
			cc.setBuffNameSelf((String) cmdRow[buffNameSelf]);
			cc.setMaxRange((float) cmdRow[maxRange]);
			cc.setHitType(HitType.getHitType((Integer) cmdRow[hitType]));
			cc.setDelayAttackEggTemplate((String) cmdRow[delayAttackEggTemplate]);
			cc.setDelayAttackParticle((String) cmdRow[delayAttackParticle]);
			cc.setInitialDelayAttackInterval((float) cmdRow[initialDelayAttackInterval]);
			cc.setDelayAttackInterval((float) cmdRow[delayAttackInterval]);
			cc.setDelayAttackLoops((int) cmdRow[delayAttackLoops]);
			cc.setEggPosition(DelayAttackEggPosition.getEggPosition((int) cmdRow[delayAttackEggPosition]));
			cc.setConeLength((float) cmdRow[coneLength]);
			cc.setAnimations(WeaponType.UNARMED, getAnimationList((String) cmdRow[animDefault + 1]));
			cc.setAnimations(WeaponType.ONE_HANDED_MELEE, getAnimationList((String) cmdRow[animDefault + 2]));
			cc.setAnimations(WeaponType.TWO_HANDED_MELEE, getAnimationList((String) cmdRow[animDefault + 3]));
			cc.setAnimations(WeaponType.POLEARM_MELEE, getAnimationList((String) cmdRow[animDefault + 4]));
			cc.setAnimations(WeaponType.POLEARM_MELEE, getAnimationList((String) cmdRow[animDefault + 5]));
			cc.setAnimations(WeaponType.PISTOL, getAnimationList((String) cmdRow[animDefault + 6]));
			cc.setAnimations(WeaponType.LIGHT_RIFLE, getAnimationList((String) cmdRow[animDefault + 7]));
			cc.setAnimations(WeaponType.CARBINE, getAnimationList((String) cmdRow[animDefault + 8]));
			cc.setAnimations(WeaponType.RIFLE, getAnimationList((String) cmdRow[animDefault + 9]));
			cc.setAnimations(WeaponType.THROWN, getAnimationList((String) cmdRow[animDefault + 10]));
			cc.setAnimations(WeaponType.ONE_HANDED_SABER, getAnimationList((String) cmdRow[animDefault + 11]));
			cc.setAnimations(WeaponType.TWO_HANDED_SABER, getAnimationList((String) cmdRow[animDefault + 12]));
			cc.setAnimations(WeaponType.POLEARM_SABER, getAnimationList((String) cmdRow[animDefault + 13]));
			commandContainer.addCommand(cc);
		}
	}
	
	private String[] getAnimationList(String cell) {
		if (cell.isEmpty())
			return new String[0];
		return cell.split(",");
	}
	
	private <T extends ICmdCallback> Command registerCallback(String command, Class<T> callback) {
		command = command.toLowerCase(Locale.ENGLISH);
		Command comand = commandContainer.getCommand(command);
		registerCallback(comand, callback);
		return comand;
	}
	
	private <T extends ICmdCallback> void registerCallback(Command command, Class<T> callback) {
		try {
			if (callback.getConstructor() == null)
				throw new IllegalArgumentException("Incorrectly registered callback class. Class must extend ICmdCallback and have an empty constructor: " + callback.getName());
		} catch (NoSuchMethodException e) {
			Log.e(e);
		}
		command.setJavaCallback(callback);
		
		List<Command> scriptCommands = commandContainer.getScriptCommandList(command.getDefaultScriptCallback());
		for (Command unregistered : scriptCommands) {
			if (unregistered != command && !unregistered.hasJavaCallback()) {
				registerCallback(unregistered, command.getJavaCallback());
			}
		}
		
	}
	
	private void registerCallbacks() {
		registerCallback("waypoint", WaypointCmdCallback.class);
		registerCallback("requestWaypointAtPosition", RequestWaypointCmdCallback.class);
		registerCallback("server", ServerCmdCallback.class);
		registerCallback("getAttributesBatch", AttributesCmdCallback.class);
		registerCallback("socialInternal", SocialInternalCmdCallback.class);
		registerCallback("sitServer", SitOnObjectCmdCallback.class);
		registerCallback("stand", StandCmdCallback.class);
		registerCallback("teleport", AdminTeleportCallback.class);
		registerCallback("prone", ProneCmdCallback.class);
		registerCallback("kneel", KneelCmdCallback.class);
		registerCallback("jumpServer", JumpCmdCallback.class);
		registerCallback("serverDestroyObject", ServerDestroyObjectCmdCallback.class);
		registerCallback("findFriend", FindFriendCallback.class);
		registerCallback("setPlayerAppearance", PlayerAppearanceCallback.class);
		registerCallback("revertPlayerAppearance", RevertAppearanceCallback.class);
		registerCallback("qatool", QaToolCmdCallback.class);
		registerCallback("goto", GotoCmdCallback.class);
		registerCallback("startDance", StartDanceCallback.class);
		registerCallback("requestBiography", RequestBiographyCmdCallback.class);
		registerCallback("flourish", FlourishCmdCallback.class);
		registerCallback("changeDance", ChangeDanceCallback.class);
		registerCallback("transferItemMisc", TransferItemCallback.class);
		registerCallback("transferItemArmor", TransferItemCallback.class);
		registerCallback("transferItemWeapon", TransferItemCallback.class);
	}
	
}
