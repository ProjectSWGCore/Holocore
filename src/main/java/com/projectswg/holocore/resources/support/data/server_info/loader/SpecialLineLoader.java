package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpecialLineLoader extends DataLoader {
	
	private final Map<String, SpecialLineInfo> specialLineMap;
	
	SpecialLineLoader() {
		specialLineMap = new HashMap<>();
	}
	
	@Nullable
	public SpecialLineInfo getSpecialLine(String name) {
		return specialLineMap.get(name);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/command/special_lines.sdb"))) {
			while (set.next()) {
				SpecialLineInfo specialLineInfo = new SpecialLineInfo(set);
				
				specialLineMap.put(specialLineInfo.getSpecialLineName(), specialLineInfo);
			}
		}
	}
	
	public static class SpecialLineInfo {
		private final String specialLineName;
		private final String cooldownModName;
		private final String addedDamageModName;	// Added damage is also used for healing abilities
		private final String criticalChanceModName;
		private final String actionCostModName;
		private final String freeshotModName;
		private final String swiftModName;
		
		SpecialLineInfo(SdbLoader.SdbResultSet set) {
			specialLineName = set.getText("line");
			cooldownModName = set.getText("cooldown");
			addedDamageModName = set.getText("damage");
			criticalChanceModName = set.getText("critical");
			actionCostModName = set.getText("action_cost");
			freeshotModName = set.getText("freeshot");
			swiftModName = set.getText("swift");
		}
		
		public String getSpecialLineName() {
			return specialLineName;
		}
		
		public String getCooldownModName() {
			return cooldownModName;
		}
		
		public String getAddedDamageModName() {
			return addedDamageModName;
		}
		
		public String getCriticalChanceModName() {
			return criticalChanceModName;
		}
		
		public String getActionCostModName() {
			return actionCostModName;
		}
		
		public String getFreeshotModName() {
			return freeshotModName;
		}
		
		public String getSwiftModName() {
			return swiftModName;
		}
	}
}
