package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.commands.callbacks.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.admin.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool.CmdQaTool;
import com.projectswg.holocore.resources.support.global.commands.callbacks.chat.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.chat.friend.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdCoupDeGrace;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdDuel;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdEndDuel;
import com.projectswg.holocore.resources.support.global.commands.callbacks.combat.CmdPVP;
import com.projectswg.holocore.resources.support.global.commands.callbacks.flags.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.generic.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.group.*;
import com.projectswg.holocore.resources.support.global.commands.callbacks.survey.CmdRequestCoreSample;
import com.projectswg.holocore.resources.support.global.commands.callbacks.survey.CmdRequestSurvey;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
	
	private void registerCallback(String callbackName, Supplier<? extends ICmdCallback> callback) {
		List<Command> commands = DataLoader.commands().getCommandByCallback(callbackName);
//		assert commands != null;
		if (commands == null) {
			Log.e("Invalid command registration: %s", callbackName);
			return;
		}
		for (Command command : commands)
			callbacks.put(command, callback);
	}
	
	private void registerCallbacks() {
		registerCallback("waypoint", WaypointCmdCallback::new);
		registerCallback("requestWaypointAtPosition", RequestWaypointCmdCallback::new);
		registerCallback("getAttributesBatch", AttributesCmdCallback::new);
		registerCallback("socialInternal", SocialInternalCmdCallback::new);
		registerCallback("sit", SitOnObjectCmdCallback::new);
		registerCallback("stand", StandCmdCallback::new);
		registerCallback("teleport", AdminTeleportCallback::new);
		registerCallback("prone", ProneCmdCallback::new);
		registerCallback("kneel", KneelCmdCallback::new);
		registerCallback("jumpServer", JumpCmdCallback::new);
		registerCallback("serverDestroyObject", ServerDestroyObjectCmdCallback::new);
		registerCallback("findFriend", FindFriendCallback::new);

		registerCallback("startDance", StartDanceCallback::new);
		registerCallback("requestBiography", RequestBiographyCmdCallback::new);
		registerCallback("flourish", FlourishCmdCallback::new);
		registerCallback("changeDance", ChangeDanceCallback::new);
		registerCallback("transferItemMisc", TransferItemCallback::new);
		registerCallback("transferItemArmor", TransferItemCallback::new);
		registerCallback("transferItemWeapon", TransferItemCallback::new);
		registerCallback("logout", LogoutCmdCallback::new);
		registerCallback("requestDraftSlots", RequestDraftSlotsCallback::new);
		
		addAdminScripts();
		addChatScripts();
		addCombatScripts();
		addFlagScripts();
		addGenericScripts();
		addGroupScripts();
		addSurveyScripts();
	}
	
	private void addAdminScripts() {
		registerCallback("dumpZoneInformation", CmdDumpZoneInformation::new);
		registerCallback("goto", CmdGoto::new);
		registerCallback("qatool", CmdQaTool::new);
		registerCallback("revertPlayerAppearance", CmdRevertPlayerAppearance::new);
		registerCallback("setGodMode", CmdSetGodMode::new);
		registerCallback("setPlayerAppearance", CmdSetPlayerAppearance::new);
		registerCallback("console_server", CmdServer::new);
		
		registerCallback("createStaticItem", CmdCreateStaticItem::new);
		registerCallback("credits", CmdMoney::new);
		registerCallback("setSpeed", CmdSetSpeed::new);
	}
	
	private void addChatScripts() {
		registerCallback("broadcast", CmdBroadcast::new);
		registerCallback("broadcastArea", CmdBroadcastArea::new);
		registerCallback("broadcastGalaxy", CmdBroadcastGalaxy::new);
		registerCallback("broadcastPlanet", CmdBroadcastPlanet::new);
		registerCallback("planetChat", CmdPlanetChat::new);
		registerCallback("spatialChatInternal", CmdSpatialChatInternal::new);
		addChatFriendScripts();
	}
	
	private void addChatFriendScripts() {
		registerCallback("addFriend", CmdAddFriend::new);
		registerCallback("addIgnore", CmdAddIgnore::new);
		registerCallback("getFriendList", CmdGetFriendList::new);
		registerCallback("removeFriend", CmdRemoveFriend::new);
		registerCallback("removeIgnore", CmdRemoveIgnore::new);
	}
	
	private void addCombatScripts() {
		registerCallback("coupDeGrace", CmdCoupDeGrace::new);
		registerCallback("duel", CmdDuel::new);
		registerCallback("endDuel", CmdEndDuel::new);
		registerCallback("pvp", CmdPVP::new);
	}
	
	private void addFlagScripts() {
		registerCallback("toggleAwayFromKeyBoard", CmdToggleAwayFromKeyboard::new);
		registerCallback("toggleDisplayingFactionRank", CmdToggleDisplayingFactionRank::new);
		registerCallback("toggleHelper", CmdToggleHelper::new);
		registerCallback("toggleLookingForGroup", CmdToggleLookingForGroup::new);
		registerCallback("toggleLookingForWork", CmdToggleLookingForWork::new);
		registerCallback("toggleOutOfCharacter", CmdToggleOutOfCharacter::new);
		registerCallback("toggleRolePlay", CmdToggleRolePlay::new); //
	}
	
	private void addGenericScripts() {
		registerCallback("grantSkill", CmdGrantSkill::new);
		registerCallback("stopDance", CmdStopDance::new);
		registerCallback("stopwatching", CmdStopWatching::new);
		registerCallback("watch", CmdWatch::new);
		registerCallback("openContainer", CmdOpenContainer::new);
		registerCallback("purchaseTicket", CmdPurchaseTicket::new);
		registerCallback("setBiography", CmdSetBiography::new);
		registerCallback("setCurrentSkillTitle", CmdSetCurrentSkillTitle::new);
		registerCallback("setMoodInternal", CmdSetMoodInternal::new);
		registerCallback("setWaypointActiveStatus", CmdSetWaypointActiveStatus::new);
		registerCallback("setWaypointName", CmdSetWaypointName::new);
	}
	
	private void addGroupScripts() {
		registerCallback("groupChat", CmdGroupChat::new);
		registerCallback("groupDecline", CmdGroupDecline::new);
		registerCallback("groupDisband", CmdGroupDisband::new);
		registerCallback("groupInvite", CmdGroupInvite::new);
		registerCallback("groupJoin", CmdGroupJoin::new);
		registerCallback("dismissGroupMember", CmdGroupKick::new);
		registerCallback("leaveGroup", CmdGroupLeave::new);
		registerCallback("groupLootSet", CmdGroupLootSet::new);
		registerCallback("groupMakeLeader", CmdGroupMakeLeader::new);
		registerCallback("groupMakeMasterLooter", CmdGroupMakeMasterLooter::new);
		registerCallback("groupUninvite", CmdGroupUninvite::new);
	}
	
	private void addSurveyScripts() {
		registerCallback("requestCoreSample", CmdRequestCoreSample::new);
		registerCallback("requestSurvey", CmdRequestSurvey::new);
	}
	
}
