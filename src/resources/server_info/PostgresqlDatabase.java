package resources.server_info;

public class PostgresqlDatabase extends RelationalDatabase {
	
	public PostgresqlDatabase(String host, String db, String user, String pass) {
		super("postgresql", host, db, user, pass, "");
	}
	
}
