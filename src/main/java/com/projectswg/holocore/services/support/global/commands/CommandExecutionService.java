/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.commands.callbacks.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.admin.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool.CmdQaTool;
import com.projectswg.holocore.resources.support.global.commands.callbacks.chat.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.chat.friend.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdAttack;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdCoupDeGrace;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdDuel;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdEndDuel;
import com.projectswg.holocore.resources.support.global.commands.callbacks.conversation.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.flags.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.generic.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.group.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.loot.CmdLoot;
import com.projectswg.holocore.resources.support.global.commands.callbacks.quest.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.survey.CmdRequestCoreSample;
import com.projectswg.holocore.resources.support.global.commands.callbacks.survey.CmdRequestSurvey;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CommandExecutionService extends Service {
	
	private final Map<Command, Supplier<? extends ICmdCallback>> callbacks;
	
	public CommandExecutionService() {
		this.callbacks = new HashMap<>();
	}
	
	@Override
	public boolean start() {
		registerCallbacks();
		return true;
	}
	
	@IntentHandler
	private void handleExecuteCommandIntent(ExecuteCommandIntent eci) {
		Supplier<? extends ICmdCallback> callbackSupplier = callbacks.get(eci.getCommand());
		if (callbackSupplier == null)
			return;
		callbackSupplier.get().execute(eci.getSource().getOwner(), eci.getTarget(), eci.getArguments());
	}
	
	private void registerCppCallback(String callbackName, Supplier<? extends ICmdCallback> callback) {
		List<Command> commands = DataLoader.Companion.commands().getCommandsByCppCallback(callbackName);
//		assert commands != null;
		if (commands == null) {
			Log.e("Invalid CPP command registration: %s", callbackName);
			return;
		}
		for (Command command : commands)
			callbacks.put(command, callback);
	}
	
	private void registerScriptCallback(String callbackName, Supplier<? extends ICmdCallback> callback) {
		List<Command> commands = DataLoader.Companion.commands().getCommandsByScriptCallback(callbackName);
//		assert commands != null;
		if (commands == null) {
			Log.e("Invalid Script command registration: %s", callbackName);
			return;
		}
		for (Command command : commands)
			callbacks.put(command, callback);
	}
	
	private void registerCallbacks() {
		registerScriptCallback("cmdWaypoint", WaypointCmdCallback::new);
		registerCppCallback("requestWaypointAtPosition", RequestWaypointCmdCallback::new);
		registerCppCallback("getAttributesBatch", AttributesCmdCallback::new);
		registerCppCallback("socialInternal", SocialInternalCmdCallback::new);
		registerScriptCallback("sit", SitOnObjectCmdCallback::new);
		registerScriptCallback("stand", StandCmdCallback::new);
		registerScriptCallback("prone", ProneCmdCallback::new);
		registerScriptCallback("kneel", KneelCmdCallback::new);
		registerCppCallback("serverDestroyObject", ServerDestroyObjectCmdCallback::new);
		registerCppCallback("findFriend", FindFriendCallback::new);
		
		registerCppCallback("requestBiography", RequestBiographyCmdCallback::new);
		registerCppCallback("requestBadges", RequestBadgesCallback::new);
		registerCppCallback("transferItemMisc", TransferItemCallback::new);
		registerCppCallback("transferItemArmor", TransferItemCallback::new);
		registerCppCallback("transferItemWeapon", TransferItemCallback::new);
		registerScriptCallback("cmdStartLogout", LogoutCmdCallback::new);
		registerCppCallback("requestDraftSlots", RequestDraftSlotsCallback::new);
		registerScriptCallback("knockdownRecovery", KnockdownRecoveryCmdCallback::new);
		registerScriptCallback("burstRun", BurstRunCmdCallback::new);
		
		addAdminScripts();
		addChatScripts();
		addCombatScripts();
		addLootScripts();
		addFlagScripts();
		addGenericScripts();
		addGroupScripts();
		addSurveyScripts();
		addConversationScripts();
		addQuestScripts();
		addEntertainerScripts();
	}
	
	private void addEntertainerScripts() {
		registerScriptCallback("cmdStartDance", StartDanceCallback::new);
		registerScriptCallback("cmdChangeDance", ChangeDanceCallback::new);
		registerScriptCallback("cmdStopDance", CmdStopDance::new);
		registerScriptCallback("cmdFlourish", FlourishCmdCallback::new);
		registerScriptCallback("cmdWatch", CmdWatch::new);
		registerScriptCallback("cmdStopWatching", CmdStopWatching::new);
	}
	
	private void addAdminScripts() {
		registerScriptCallback("dumpZoneInformation", CmdDumpZoneInformation::new);
		registerScriptCallback("cmdGoto", CmdGoto::new);
		registerCppCallback("console_npc", CmdNpc::new);
		registerScriptCallback("createNPC", CmdCreateNpc::new);
		registerScriptCallback("cmdQaTool", CmdQaTool::new);
		registerCppCallback("admin_setGodMode", CmdSetGodMode::new);
		registerCppCallback("setPlayerAppearance", CmdSetPlayerAppearance::new);
		registerCppCallback("console_server", CmdServer::new);
		registerCppCallback("admin_teleport", AdminTeleportCallback::new);
		
		registerScriptCallback("cmdCreateStaticItem", CmdCreateStaticItem::new);
		registerCppCallback("console_money", CmdMoney::new);
		registerScriptCallback("cmdSetSpeed", CmdSetSpeed::new);
		registerScriptCallback("cmdSetExperience", CmdSetExperience::new);
		registerScriptCallback("cmdInvulnerable", CmdInvulnerable::new);
		registerScriptCallback("cmdGrantSkill", CmdGrantSkill::new);
	}
	
	private void addChatScripts() {
		registerScriptCallback("cmdBroadcast", CmdBroadcast::new);
		registerScriptCallback("cmdBroadcastArea", CmdBroadcastArea::new);
		registerScriptCallback("cmdBroadcastGalaxy", CmdBroadcastGalaxy::new);
		registerScriptCallback("cmdBroadcastPlanet", CmdBroadcastPlanet::new);
		registerCppCallback("planetChat", CmdPlanetChat::new);
		registerCppCallback("spatialChatInternal", CmdSpatialChatInternal::new);
		registerCppCallback("setSpokenLanguage", CmdSetSpokenLanguage::new);
		addChatFriendScripts();
	}
	
	private void addChatFriendScripts() {
		registerCppCallback("addFriend", CmdAddFriend::new);
		registerCppCallback("addIgnore", CmdAddIgnore::new);
		registerCppCallback("getFriendList", CmdGetFriendList::new);
		registerCppCallback("removeFriend", CmdRemoveFriend::new);
		registerCppCallback("removeIgnore", CmdRemoveIgnore::new);
	}
	
	private void addCombatScripts() {
		registerScriptCallback("cmdCoupDeGrace", CmdCoupDeGrace::new);
		registerCppCallback("duel", CmdDuel::new);
		registerCppCallback("endDuel", CmdEndDuel::new);
		registerScriptCallback("attack", CmdAttack::new);
	}
	
	private void addLootScripts() {
		registerScriptCallback("cmdLoot", CmdLoot::new);
	}
	
	private void addFlagScripts() {
		registerCppCallback("toggleAwayFromKeyBoard", CmdToggleAwayFromKeyboard::new);
		registerCppCallback("toggleDisplayingFactionRank", CmdToggleDisplayingFactionRank::new);
		registerCppCallback("toggleHelper", CmdToggleHelper::new);
		registerCppCallback("toggleLookingForGroup", CmdToggleLookingForGroup::new);
		registerCppCallback("toggleRolePlay", CmdToggleRolePlay::new);
	}
	
	private void addGenericScripts() {
		registerCppCallback("surrenderSkill", CmdSurrenderSkill::new);
		registerCppCallback("openContainer", CmdOpenContainer::new);
		registerCppCallback("closeContainer", CmdCloseContainer::new);
		registerCppCallback("purchaseTicket", CmdPurchaseTicket::new);
		registerCppCallback("setBiography", CmdSetBiography::new);
		registerCppCallback("setCurrentSkillTitle", CmdSetCurrentSkillTitle::new);
		registerCppCallback("setMoodInternal", CmdSetMoodInternal::new);
		registerCppCallback("setWaypointActiveStatus", CmdSetWaypointActiveStatus::new);
		registerCppCallback("setWaypointName", CmdSetWaypointName::new);
	}
	
	private void addGroupScripts() {
		registerCppCallback("groupChat", CmdGroupChat::new);
		registerCppCallback("groupDecline", CmdGroupDecline::new);
		registerCppCallback("groupDisband", CmdGroupDisband::new);
		registerCppCallback("groupInvite", CmdGroupInvite::new);
		registerCppCallback("groupJoin", CmdGroupJoin::new);
		registerScriptCallback("cmdDismissGroupMember", CmdGroupKick::new);
		registerScriptCallback("cmdLeaveGroup", CmdGroupLeave::new);
		registerScriptCallback("cmdGroupLootSet", CmdGroupLootSet::new);
		registerCppCallback("groupMakeLeader", CmdGroupMakeLeader::new);
		registerCppCallback("groupMakeMasterLooter", CmdGroupMakeMasterLooter::new);
		registerCppCallback("groupUninvite", CmdGroupUninvite::new);
	}
	
	private void addSurveyScripts() {
		registerCppCallback("requestCoreSample", CmdRequestCoreSample::new);
		registerCppCallback("requestSurvey", CmdRequestSurvey::new);
	}
	
	private void addConversationScripts() {
		registerCppCallback("npcConversationStart", NpcConversationStartCmdCallback::new);
		registerCppCallback("npcConversationSelect", NpcConversationSelectCmdCallback::new);
		registerCppCallback("npcConversationStop", NpcConversationStopCmdCallback::new);
	}
	
	private void addQuestScripts() {
		registerCppCallback("abandonQuest", CmdAbandonQuest::new);
		registerScriptCallback("cmdCompleteQuest", CmdCompleteQuest::new);
	}
	
}
