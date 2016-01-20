package services.galaxy.terminals;

import intents.radial.ObjectClickedIntent;
import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import resources.control.Intent;
import resources.control.Service;
import resources.radial.RadialItem;
import resources.radial.RadialOption;
import resources.radial.Radials;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class TerminalService extends Service {
	
	private static final String GET_ALL_TEMPLATES_SQL = "SELECT iff FROM radials";
	private static final String GET_SCRIPT_FOR_IFF_SQL = "SELECT script FROM radials WHERE iff = ?";
	
	private final Set<String> templates;
	private final RelationalServerData iffDatabase;
	private final PreparedStatement getAllTemplatesStatement;
	private final PreparedStatement getScriptForIffStatement;
	
	public TerminalService() {
		templates = new HashSet<>();
		iffDatabase = RelationalServerFactory.getServerData("radial/radials.db", "radials");
		if (iffDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");

		getAllTemplatesStatement = iffDatabase.prepareStatement(GET_ALL_TEMPLATES_SQL);
		getScriptForIffStatement = iffDatabase.prepareStatement(GET_SCRIPT_FOR_IFF_SQL);
		
		registerForIntent(RadialRequestIntent.TYPE);
		registerForIntent(RadialSelectionIntent.TYPE);
		registerForIntent(ObjectClickedIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
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
			case ObjectClickedIntent.TYPE:
				if (i instanceof ObjectClickedIntent) {
					ObjectClickedIntent oci = (ObjectClickedIntent) i;
					String script = lookupScript(oci.getTarget().getTemplate());
					if (script == null)
						return;
					List<RadialOption> options = Radials.getRadialOptions(script, oci.getRequestor().getOwner(), oci.getTarget());
					if (options.isEmpty())
						return;
					RadialItem item = RadialItem.getFromId(options.get(0).getId());
					Radials.handleSelection(script, oci.getRequestor().getOwner(), oci.getTarget(), item);
				}
				break;
		}
	}
	
	private String lookupScript(String iff) {
		if (!templates.contains(iff))
			return null;
		synchronized (getScriptForIffStatement) {
			ResultSet set = null;
			try {
				getScriptForIffStatement.setString(1, iff);
				set = getScriptForIffStatement.executeQuery();
				if (set.next())
					return set.getString("script");
				else
					Log.e("TerminalService", "Cannot find script for template: " + iff);
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
