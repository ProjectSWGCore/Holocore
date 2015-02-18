package services.player;

import intents.GalacticIntent;
import intents.LoginEventIntent;
import intents.LoginEventIntent.LoginEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import main.ProjectSWG;
import network.encryption.MD5;
import network.packets.Packet;
import network.packets.soe.SessionRequest;
import network.packets.swg.ErrorMessage;
import network.packets.swg.ServerUnixEpochTime;
import network.packets.swg.login.CharacterCreationDisabled;
import network.packets.swg.login.EnumerateCharacterId;
import network.packets.swg.login.EnumerateCharacterId.SWGCharacter;
import network.packets.swg.login.creation.DeleteCharacterRequest;
import network.packets.swg.login.creation.DeleteCharacterResponse;
import network.packets.swg.login.LoginClientId;
import network.packets.swg.login.LoginClientToken;
import network.packets.swg.login.LoginClusterStatus;
import network.packets.swg.login.LoginEnumCluster;
import network.packets.swg.login.ServerId;
import network.packets.swg.login.ServerString;
import network.packets.swg.login.StationIdHasJediSlot;
import resources.Galaxy;
import resources.Race;
import resources.Galaxy.GalaxyStatus;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.RelationalDatabase;
import resources.services.Config;

public class LoginService extends Service {
	
	private static final String REQUIRED_VERSION = "20111130-15:46";
	
	private Random random;
	private PreparedStatement getUser;
	private PreparedStatement getUserInsensitive;
	private PreparedStatement getGalaxies;
	private PreparedStatement getCharacters;
	private PreparedStatement deleteCharacter;
	private boolean autoLogin;
	
	public LoginService() {
		random = new Random();
	}
	
	@Override
	public boolean initialize() {
		RelationalDatabase local = getLocalDatabase();
		getUser = local.prepareStatement("SELECT * FROM users WHERE username = ?");
		getUserInsensitive = local.prepareStatement("SELECT * FROM users WHERE username ilike ?");
		getGalaxies = local.prepareStatement("SELECT * FROM galaxies");
		getCharacters = local.prepareStatement("SELECT * FROM characters WHERE userid = ?");
		deleteCharacter = local.prepareStatement("DELETE FROM CHARACTERS WHERE id = ?");
		autoLogin = (getConfig(ConfigFile.PRIMARY).getInt("AUTO-LOGIN", 0) == 1 ? true : false);
		return super.initialize();
	}
	
	public void handlePacket(GalacticIntent intent, Player player, Packet p) {
		if (p instanceof SessionRequest) {
			player.setConnectionId(((SessionRequest)p).getConnectionID());
			player.setPlayerState(PlayerState.DISCONNECTED);
			sendServerInfo(player);
		}
		if (p instanceof LoginClientId)
			handleLogin(player, (LoginClientId) p);
		if (p instanceof DeleteCharacterRequest)
			handleCharDeletion(intent, player, (DeleteCharacterRequest) p);
	}
	
	private void sendServerInfo(Player player) {
		Config c = getConfig(ConfigFile.PRIMARY);
		String name = c.getString("LOGIN-SERVER-NAME", "LoginServer");
		int id = c.getInt("LOGIN-SERVER-ID", 1);
		sendPacket(player.getNetworkId(), new ServerString(name + ":" + id));
		sendPacket(player.getNetworkId(), new ServerId(id));
	}
	
	private void handleCharDeletion(GalacticIntent intent, Player player, DeleteCharacterRequest request) {
		SWGObject obj = intent.getObjectManager().deleteObject(request.getPlayerId());
		if (obj != null && obj instanceof CreatureObject)
			System.out.println("[" + player.getUsername() + "] Delete Character: " + ((CreatureObject)obj).getName() + ". IP: " + request.getAddress() + ":" + request.getPort());
		sendPacket(player, new DeleteCharacterResponse(deleteCharacter(request.getPlayerId())));
	}
	
	private void handleLogin(Player player, LoginClientId id) {
		if (player.getPlayerState() != PlayerState.DISCONNECTED) {
			System.err.println("Player cannot login when " + player.getPlayerState());
			return;
		}
		if (!id.getVersion().equals(REQUIRED_VERSION)) {
			onLoginClientVersionError(player, id);
			return;
		}
		if (autoLogin) {
			String [] sessionHash = id.getPassword().split("-");
			id.setUsername(sessionHash[0]);
			id.setPassword(sessionHash[1]);
		}
		try {
			ResultSet user = getUser(id.getUsername());
			if (user.next()) {
				if (isUserValid(user, id.getPassword()))
					onSuccessfulLogin(user, player, id);
				else if (user.getBoolean("banned"))
					onLoginBanned(player, id);
				else
					onInvalidUserPass(player, id);
			} else
				onInvalidUserPass(player, id);
		} catch (SQLException e) {
			e.printStackTrace();
			onLoginServerError(player);
		}
	}
	
	private void onLoginClientVersionError(Player player, LoginClientId id) {
		System.err.println("LoginService: " + id.getUsername() + " cannot login due to invalid version code: " + id.getVersion());
		String type = "Login Failed!";
		String message = "Invalid Client Version Code: " + id.getVersion();
		sendPacket(player.getNetworkId(), new ErrorMessage(type, message, false));
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_VERSION_CODE).broadcast();
	}
	
	private void onSuccessfulLogin(ResultSet user, Player player, LoginClientId id) throws SQLException {
		player.setUsername(id.getUsername());
		player.setUserId(user.getInt("id"));
		int tokenLength = getConfig(ConfigFile.PRIMARY).getInt("SESSION-TOKEN-LENGTH", 24);
		byte [] sessionToken = new byte[tokenLength];
		random.nextBytes(sessionToken);
		player.setSessionToken(sessionToken);
		player.setPlayerState(PlayerState.LOGGING_IN);
		switch(user.getString("access_level")) {
			case "player": player.setAccessLevel(AccessLevel.PLAYER); break;
			case "admin": player.setAccessLevel(AccessLevel.ADMIN); break;
			case "dev": player.setAccessLevel(AccessLevel.DEV); break;
			case "qa": player.setAccessLevel(AccessLevel.QA); break;
			default: player.setAccessLevel(AccessLevel.PLAYER); break;
		}
		sendLoginSuccessPacket(player);
		System.out.println("[" + player.getUsername() + "] Connected to the login server. IP: " + id.getAddress() + ":" + id.getPort());
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_SUCCESS).broadcast();
	}
	
	private void onLoginBanned(Player player, LoginClientId id) {
		String type = "Login Failed!";
		String message = "Sorry, you're banned!";
		sendPacket(player.getNetworkId(), new ErrorMessage(type, message, false));
		System.err.println("[" + id.getUsername() + "] Can't login - Banned! IP: " + id.getAddress() + ":" + id.getPort());
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_BANNED).broadcast();
	}
	
	private void onInvalidUserPass(Player player, LoginClientId id) {
		String type = "Login Failed!";
		String message = "Invalid username or password.";
		try {
			String similar = getSimilarUsername(id.getUsername());
			if (similar != null && !similar.equals(id.getUsername()))
				message += "\n\nDid you mean: '" + similar + "'?";
		} catch (SQLException e) {
			
		}
		message += "\n\nMake sure to login to forums first!";
		sendPacket(player.getNetworkId(), new ErrorMessage(type, message, false));
		System.err.println("[" + id.getUsername() + "] Invalid user/pass combo! IP: " + id.getAddress() + ":" + id.getPort());
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_USER_PASS).broadcast();
	}
	
	private void onLoginServerError(Player player) {
		String type = "Login Failed!";
		String message = "Server Error.";
		sendPacket(player.getNetworkId(), new ErrorMessage(type, message, false));
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_SERVER_ERROR).broadcast();
	}
	
	private String getSimilarUsername(String user) throws SQLException {
		ResultSet set = null;
		try {
			getUserInsensitive.setString(1, user);
			set = getUserInsensitive.executeQuery();
			String similar = null;
			if (set.next())
				similar = set.getString("username");
			set.close();
			return similar;
		} finally {
			if (set != null)
				set.close();
		}
	}
	
	private void sendLoginSuccessPacket(Player p) throws SQLException {
		LoginClientToken token = new LoginClientToken(p.getSessionToken(), p.getUserId(), p.getUsername());
		LoginEnumCluster cluster = new LoginEnumCluster();
		LoginClusterStatus clusterStatus = new LoginClusterStatus();
		List <Galaxy> galaxies = getGalaxies(p);
		SWGCharacter [] characters = getCharacters(p.getUserId());
		for (Galaxy g : galaxies) {
			cluster.addGalaxy(g);
			clusterStatus.addGalaxy(g);
		}
		cluster.setMaxCharacters(getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 2));
		sendPacket(p.getNetworkId(), new ServerUnixEpochTime((int) (ProjectSWG.getCoreTime() / 1000)));
		sendPacket(p.getNetworkId(), token);
		sendPacket(p.getNetworkId(), cluster);
		sendPacket(p.getNetworkId(), new CharacterCreationDisabled());
		sendPacket(p.getNetworkId(), clusterStatus);
		sendPacket(p.getNetworkId(), new StationIdHasJediSlot(0));
		sendPacket(p.getNetworkId(), new EnumerateCharacterId(characters));
		p.setPlayerState(PlayerState.LOGGED_IN);
	}
	
	private boolean isUserValid(ResultSet set, String password) throws SQLException {
		if (password.isEmpty())
			return false;
		if (set.getBoolean("banned"))
			return false;
		if (set.getString("password").equals(password))
			return true;
		password = MD5.digest(MD5.digest(set.getString("password_salt")) + MD5.digest(password));
		return set.getString("password").equals(password);
	}
	
	private ResultSet getUser(String username) throws SQLException {
		getUser.setString(1, username);
		return getUser.executeQuery();
	}
	
	public ResultSet getCharacter(String character) throws SQLException {
		PreparedStatement statement = getLocalDatabase().prepareStatement("SELECT * FROM characters WHERE lower(name) = ?");
		statement.setString(1, character.toLowerCase(Locale.ENGLISH));
		return statement.executeQuery();
	}
	
	private List <Galaxy> getGalaxies(Player p) throws SQLException {
		Config c = getConfig(ConfigFile.PRIMARY);
		ResultSet set = getGalaxies.executeQuery();
		List <Galaxy> galaxies = new ArrayList<Galaxy>();
		try {
			while (set.next()) {
				Galaxy g = new Galaxy();
				g.setId(set.getInt("id"));
				g.setName(set.getString("name"));
				g.setAddress(set.getString("address"));
				g.setPopulation(set.getInt("population"));
				g.setTimeZone(set.getInt("timezone"));
				g.setZonePort(set.getInt("zone_port"));
				g.setPingPort(set.getInt("ping_port"));
				g.setStatus(set.getInt("status"));
				g.setMaxCharacters(c.getInt("GALAXY-MAX-CHARACTERS", 2));
				g.setOnlinePlayerLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
				g.setOnlineFreeTrialLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
				g.setRecommended(true);
				// If locked, restricted, or full
				if (p.getAccessLevel() == AccessLevel.ADMIN && g.getStatus() != GalaxyStatus.UP)
					g.setStatus(GalaxyStatus.UP);
				galaxies.add(g);
			}
			set.close();
			return galaxies;
		} finally {
			set.close();
		}
	}
	
	private SWGCharacter [] getCharacters(int userId) throws SQLException {
		getCharacters.setInt(1, userId);
		ResultSet set = getCharacters.executeQuery();
		List <SWGCharacter> characters = new ArrayList<SWGCharacter>();
		try {
			while (set.next()) {
				SWGCharacter c = new SWGCharacter();
				c.setId(set.getInt("id"));
				c.setName(set.getString("name"));
				c.setRaceCrc(Race.getRaceByFile(set.getString("race")).getCrc());
				c.setGalaxyId(set.getInt("galaxyid"));
				c.setType(1); // 1 = Normal (2 = Jedi, 3 = Spectral)
				characters.add(c);
			}
		} finally {
			set.close();
		}
		return characters.toArray(new SWGCharacter[characters.size()]);
	}
	
	private boolean deleteCharacter(long id) {
		synchronized (deleteCharacter) {
			try {
				deleteCharacter.setLong(1, id);
				return deleteCharacter.executeUpdate() > 0;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
}
