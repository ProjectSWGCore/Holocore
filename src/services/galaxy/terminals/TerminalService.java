package services.galaxy.terminals;

import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;

import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import resources.control.Intent;
import resources.control.Service;
import resources.radial.RadialOption;
import resources.radial.Radials;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;

public class TerminalService extends Service {
	
	private static final String GET_ALL_TEMPLATES_SQL = "SELECT iff FROM iff_to_script";
	private static final String GET_SCRIPT_FOR_IFF_SQL = "SELECT script FROM iff_to_script WHERE iff = ?";
	
	private final Set<String> templates;
	private final RelationalServerData iffDatabase;
	private final PreparedStatement getAllTemplatesStatement;
	private final PreparedStatement getScriptForIffStatement;
	
	public TerminalService() {
		templates = new HashSet<>();
		iffDatabase = new RelationalServerData("serverdata/radial/radials.db");

		if (!iffDatabase.linkTableWithSdb("iff_to_script", "serverdata/radial/radials.sdb"))
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");

		getAllTemplatesStatement = iffDatabase.prepareStatement(GET_ALL_TEMPLATES_SQL);
		getScriptForIffStatement = iffDatabase.prepareStatement(GET_SCRIPT_FOR_IFF_SQL);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(RadialRequestIntent.TYPE);
		registerForIntent(RadialSelectionIntent.TYPE);
		synchronized (getAllTemplatesStatement) {
			// Cool and fancy Java thing to auto-cleanup resources
			try (ResultSet set = getAllTemplatesStatement.executeQuery()) {
				templates.clear();
				while (set.next()) {
					templates.add(set.getString("iff"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		new RadialRegisterIntent(templates, true).broadcast();
		return super.start();
	}
	
	@Override
	public boolean stop() {
		new RadialRegisterIntent(templates, false).broadcast();
		return super.stop();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case RadialRequestIntent.TYPE:
				if (i instanceof RadialRequestIntent) {
					RadialRequestIntent rri = (RadialRequestIntent) i;
					String script = lookupScript(rri.getTarget().getTemplate());
					if (script == null)
						return;
					List<RadialOption> options = Radials.getRadialOptions(script, rri.getPlayer(), rri.getTarget());
					new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
				}
				break;
			case RadialSelectionIntent.TYPE:
				if (i instanceof RadialSelectionIntent) {
					RadialSelectionIntent rsi = (RadialSelectionIntent) i;
					String script = lookupScript(rsi.getTarget().getTemplate());
					if (script == null)
						return;
					Radials.handleSelection(script, rsi.getPlayer(), rsi.getTarget(), rsi.getSelection());
				}
				break;
		}
	}
	
	private String lookupScript(String iff) {
		synchronized (getScriptForIffStatement) {
			ResultSet set = null;
			try {
				getScriptForIffStatement.setString(1, iff);
				set = getScriptForIffStatement.executeQuery();
				if (set.next())
					return set.getString("script");
				else
					Log.e("RadialService", "Cannot find script for template: " + iff);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (set != null) {
					try {
						set.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
}
