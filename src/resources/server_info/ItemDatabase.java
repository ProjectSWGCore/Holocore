package resources.server_info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import resources.client_info.ClientFactory;

public class ItemDatabase implements AutoCloseable{
	private static final String GET_IFF_TEMPLATE_SQL = "SELECT iff_template FROM master_item where item_name = ?";
	
	private RelationalServerData database;
	private PreparedStatement getIffTemplateStatement;
	
	public ItemDatabase() {
		database = RelationalServerFactory.getServerData("items/master_item.db", "master_item");
		if (database == null)
			throw new main.ProjectSWG.CoreException("Database master_item failed to load");
		
		getIffTemplateStatement = database.prepareStatement(GET_IFF_TEMPLATE_SQL);
	}
	
	
	@Override
	public void close() throws Exception {
		try {
			getIffTemplateStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	public String getIffTemplate(String item_name){

		synchronized (getIffTemplateStatement) {
			try{
				getIffTemplateStatement.setString(1, item_name);

				try (ResultSet set = getIffTemplateStatement.executeQuery()) {
					if (set.next()){
						return ClientFactory.formatToSharedFile(set.getString(set.findColumn("iff_template")));
					}
				}
			}catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return "";		
	}

}
