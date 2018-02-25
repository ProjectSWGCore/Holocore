/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.commands;

import java.io.File;
import java.util.List;
import java.util.Locale;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.CRC;
import com.projectswg.common.data.combat.AttackType;
import com.projectswg.common.data.combat.DamageType;
import com.projectswg.common.data.combat.DelayAttackEggPosition;
import com.projectswg.common.data.combat.HitType;
import com.projectswg.common.data.combat.ValidTarget;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue;

import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.intents.player.PlayerTransformedIntent;
import com.projectswg.holocore.resources.commands.CombatCommand;
import com.projectswg.holocore.resources.commands.Command;
import com.projectswg.holocore.resources.commands.DefaultPriority;
import com.projectswg.holocore.resources.commands.ICmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.AdminTeleportCallback;
import com.projectswg.holocore.resources.commands.callbacks.AttributesCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.ChangeDanceCallback;
import com.projectswg.holocore.resources.commands.callbacks.FindFriendCallback;
import com.projectswg.holocore.resources.commands.callbacks.FlourishCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.GotoCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.JumpCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.KneelCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.LogoutCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.PlayerAppearanceCallback;
import com.projectswg.holocore.resources.commands.callbacks.ProneCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.QaToolCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.RequestBiographyCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.RequestDraftSlotsCallback;
import com.projectswg.holocore.resources.commands.callbacks.RequestWaypointCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.RevertAppearanceCallback;
import com.projectswg.holocore.resources.commands.callbacks.ServerCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.ServerDestroyObjectCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.SitOnObjectCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.SocialInternalCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.StandCmdCallback;
import com.projectswg.holocore.resources.commands.callbacks.StartDanceCallback;
import com.projectswg.holocore.resources.commands.callbacks.TransferItemCallback;
import com.projectswg.holocore.resources.commands.callbacks.WaypointCmdCallback;
import com.projectswg.holocore.resources.config.ConfigFile;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponType;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.server_info.BasicLogStream;
import com.projectswg.holocore.resources.server_info.DataManager;
import com.projectswg.holocore.scripts.commands.admin.CmdDumpZoneInformation;
import com.projectswg.holocore.scripts.commands.admin.CmdSetGodMode;
import com.projectswg.holocore.scripts.commands.admin.CmdSetSpeed;
import com.projectswg.holocore.scripts.commands.chat.*;
import com.projectswg.holocore.scripts.commands.chat.friend.*;
import com.projectswg.holocore.scripts.commands.combat.CmdCoupDeGrace;
import com.projectswg.holocore.scripts.commands.combat.CmdDuel;
import com.projectswg.holocore.scripts.commands.combat.CmdEndDuel;
import com.projectswg.holocore.scripts.commands.combat.CmdPVP;
import com.projectswg.holocore.scripts.commands.flags.*;
import com.projectswg.holocore.scripts.commands.generic.*;
import com.projectswg.holocore.scripts.commands.group.*;
import com.projectswg.holocore.scripts.commands.survey.CmdRequestCoreSample;
import com.projectswg.holocore.scripts.commands.survey.CmdRequestSurvey;
import com.projectswg.holocore.services.commands.CommandLauncher.EnqueuedCommand;
import com.projectswg.holocore.services.galaxy.GalacticManager;

public class CommandService extends Service {
	
	private final CommandContainer	commandContainer;
	private final CommandLauncher	commandLauncher;
	private final BasicLogStream	commandLogger;
	
	public CommandService() {
		this.commandContainer = new CommandContainer();
		this.commandLauncher = new CommandLauncher();
		this.commandLogger = new BasicLogStream(new File("log/commands.txt"));
		
		registerForIntent(GalacticPacketIntent.class, this::handleGalacticPacketIntent);
		registerForIntent(PlayerEventIntent.class, this::handlePlayerEventIntent);
		registerForIntent(PlayerTransformedIntent.class, this::handlePlayerTransformedIntent);
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
		SWGPacket p = gpi.getPacket();
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
		if (isCommandLogging())
			commandLogger.log("%-25s[from: %s, script: %s, target: %s]", command.getName(), player.getCreatureObject().getObjectName(), command.getDefaultScriptCallback(), target);
		
		EnqueuedCommand enqueued = new EnqueuedCommand(command, galacticManager, target, request);
		if (!command.getCooldownGroup().equals("defaultCooldownGroup") && command.isAddToCombatQueue()) {
			commandLauncher.addToQueue(player, enqueued);
		} else {
			// Execute it now
			commandLauncher.doCommand(player, enqueued);
		}
	}
	
	private boolean isCommandLogging() {
		return DataManager.getConfig(ConfigFile.DEBUG).getBoolean("COMMAND-LOGGING", true);
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
	
	private void registerCallback(String command, ICmdCallback callback) {
		command = command.toLowerCase(Locale.ENGLISH);
		Command comand = commandContainer.getCommand(command);
		registerCallback(comand, callback);
	}
	
	private void registerCallback(Command command, ICmdCallback callback) {
		command.setJavaCallback(callback);
		
		List<Command> scriptCommands = commandContainer.getScriptCommandList(command.getDefaultScriptCallback());
		for (Command unregistered : scriptCommands) {
			if (unregistered != command && !unregistered.hasJavaCallback()) {
				registerCallback(unregistered, command.getJavaCallback());
			}
		}
		
	}
	
	private void registerCallbacks() {
		registerCallback("waypoint", new WaypointCmdCallback());
		registerCallback("requestWaypointAtPosition", new RequestWaypointCmdCallback());
		registerCallback("getAttributesBatch", new AttributesCmdCallback());
		registerCallback("socialInternal", new SocialInternalCmdCallback());
		registerCallback("sitServer", new SitOnObjectCmdCallback());
		registerCallback("stand", new StandCmdCallback());
		registerCallback("teleport", new AdminTeleportCallback());
		registerCallback("prone", new ProneCmdCallback());
		registerCallback("kneel", new KneelCmdCallback());
		registerCallback("jumpServer", new JumpCmdCallback());
		registerCallback("serverDestroyObject", new ServerDestroyObjectCmdCallback());
		registerCallback("findFriend", new FindFriendCallback());

		registerCallback("startDance", new StartDanceCallback());
		registerCallback("requestBiography", new RequestBiographyCmdCallback());
		registerCallback("flourish", new FlourishCmdCallback());
		registerCallback("changeDance", new ChangeDanceCallback());
		registerCallback("transferItemMisc", new TransferItemCallback());
		registerCallback("transferItemArmor", new TransferItemCallback());
		registerCallback("transferItemWeapon", new TransferItemCallback());
		registerCallback("logout", new LogoutCmdCallback());
		registerCallback("requestDraftSlots", new RequestDraftSlotsCallback());
		
		// Scripts
		addAdminScripts();
		addChatScripts();
		addCombatScripts();
		addFlagScripts();
		addGroupScripts();
		addSurveyScripts();
		registerCallback("stopDance", CmdStopDance::execute);
		registerCallback("stopwatching", CmdStopWatching::execute);
		registerCallback("tip", CmdTip::execute);
		registerCallback("watch", CmdWatch::execute);
		registerCallback("openContainer", CmdOpenContainer::execute);
		registerCallback("purchaseTicket", CmdPurchaseTicket::execute);
		registerCallback("setBiography", CmdSetBiography::execute);
		registerCallback("setCurrentSkillTitle", CmdSetCurrentSkillTitle::execute);
		registerCallback("setMoodInternal", CmdSetMoodInternal::execute);
		registerCallback("setWaypointActiveStatus", CmdSetWaypointActiveStatus::execute);
		registerCallback("setWaypointName", CmdSetWaypointName::execute);
	}
	
	private void addAdminScripts() {
		registerCallback("dumpZoneInformation", CmdDumpZoneInformation::execute);
		registerCallback("goto", new GotoCmdCallback());
		registerCallback("grantSkill", CmdGrantSkill::execute);
		registerCallback("qatool", new QaToolCmdCallback());
		registerCallback("revertPlayerAppearance", new RevertAppearanceCallback());
		registerCallback("setGodMode", CmdSetGodMode::execute);
		registerCallback("setPlayerAppearance", new PlayerAppearanceCallback());
		registerCallback("server", new ServerCmdCallback());
		
		registerCallback("setSpeed", CmdSetSpeed::execute);
	}
	
	private void addChatScripts() {
		registerCallback("broadcast", CmdBroadcast::execute);
		registerCallback("broadcastArea", CmdBroadcastArea::execute);
		registerCallback("broadcastGalaxy", CmdBroadcastGalaxy::execute);
		registerCallback("broadcastPlanet", CmdBroadcastPlanet::execute);
		registerCallback("planet", CmdPlanetChat::execute);
		registerCallback("spatialChatInternal", CmdSpatialChatInternal::execute);
		addChatFriendScripts();
	}
	
	private void addChatFriendScripts() {
		registerCallback("addFriend", CmdAddFriend::execute);
		registerCallback("addIgnore", CmdAddIgnore::execute);
		registerCallback("getFriendList", CmdGetFriendList::execute);
		registerCallback("removeFriend", CmdRemoveFriend::execute);
		registerCallback("removeIgnore", CmdRemoveIgnore::execute);
	}
	
	private void addCombatScripts() {
		registerCallback("coupDeGrace", CmdCoupDeGrace::execute);
		registerCallback("deathBlow", CmdCoupDeGrace::execute);
		registerCallback("duel", CmdDuel::execute);
		registerCallback("endDuel", CmdEndDuel::execute);
		registerCallback("pvp", CmdPVP::execute);
	}
	
	private void addFlagScripts() {
		registerCallback("toggleAwayFromKeyBoard", CmdToggleAwayFromKeyBoard::execute);
		registerCallback("toggleDisplayingFactionRank", CmdToggleDisplayingFactionRank::execute);
		registerCallback("newbiehelper", CmdToggleHelper::execute);
		registerCallback("lfg", CmdToggleLookingForGroup::execute);
		registerCallback("lfw", CmdToggleLookingForWork::execute);
		registerCallback("ooc", CmdToggleOutOfCharacter::execute);
		registerCallback("rolePlay", CmdToggleRolePlay::execute);
	}
	
	private void addGroupScripts() {
		registerCallback("groupChat", CmdGroupChat::execute);
		registerCallback("decline", CmdGroupDecline::execute);
		registerCallback("disband", CmdGroupDisband::execute);
		registerCallback("invite", CmdGroupInvite::execute);
		registerCallback("join", CmdGroupJoin::execute);
		registerCallback("dismissGroupMember", CmdGroupKick::execute);
		registerCallback("leaveGroup", CmdGroupLeave::execute);
		registerCallback("groupLoot", CmdGroupLootSet::execute);
		registerCallback("makeLeader", CmdGroupMakeLeader::execute);
		registerCallback("makeMasterLooter", CmdGroupMakeMasterLooter::execute);
		registerCallback("uninvite", CmdGroupUninvite::execute);
	}
	
	private void addSurveyScripts() {
		registerCallback("requestCoreSample", CmdRequestCoreSample::execute);
		registerCallback("requestSurvey", CmdRequestSurvey::execute);
	}
	
}
