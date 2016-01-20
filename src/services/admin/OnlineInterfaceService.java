package services.admin;

import intents.PlayerEventIntent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import resources.common.BCrypt;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.server_info.Config;
import resources.server_info.Log;
import services.admin.http.HttpServer;
import services.admin.http.HttpServer.HttpServerCallback;
import services.admin.http.HttpSocket;
import services.admin.http.HttpSocket.HttpRequest;
import services.admin.http.HttpSession;
import services.admin.http.HttpsServer;
import utilities.ThreadUtilities;

public class OnlineInterfaceService extends Service implements HttpServerCallback {
	
	private static final String TAG = "OnlineInterfaceService";
	private static final String GET_USER_SQL = "SELECT password, banned FROM users WHERE LOWER(username) = LOWER(?)";
	
	private final WebserverData data;
	private final WebserverHandler handler;
	private final Runnable dataCollectionRunnable;
	private final PreparedStatement getUser;
	private ScheduledExecutorService executor;
	private HttpsServer httpsServer;
	private HttpServer httpServer;
	private boolean authorized;
	
	public OnlineInterfaceService() {
		data = new WebserverData();
		handler = new WebserverHandler(data);
		dataCollectionRunnable = () -> collectData();
		getUser = getLocalDatabase().prepareStatement(GET_USER_SQL);
		authorized = false;
	}
	
	@Override
	public boolean initialize() {
		Config network = getConfig(ConfigFile.NETWORK);
		authorized = !network.getString("HTTPS-KEYSTORE-PASSWORD", "").isEmpty();
		if (!authorized)
			return super.initialize();
		httpServer = new HttpServer(getBindAddr(network, "HTTP-BIND-ADDR", "BIND-ADDR"), network.getInt("HTTP-PORT", 8080));
		httpsServer = new HttpsServer(getBindAddr(network, "HTTPS-BIND-ADDR", "BIND-ADDR"), network.getInt("HTTPS-PORT", 8081));
		httpServer.setMaxConnections(network.getInt("HTTP-MAX-CONNECTIONS", 2));
		httpsServer.setMaxConnections(network.getInt("HTTPS-MAX-CONNECTIONS", 5));
		if (!httpsServer.initialize(network)) {
			System.err.println("Failed to initialize HTTPS server! Incorrect password?");
			httpServer.stop();
			httpsServer.stop();
			super.initialize();
			return false;
		}
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("ServerInterface-DataCollection"));
		registerForIntent(PlayerEventIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		if (authorized) {
			httpServer.setServerCallback(this);
			httpsServer.setServerCallback(this);
			httpServer.start();
			httpsServer.start();
			executor.scheduleAtFixedRate(dataCollectionRunnable, 0, 1, TimeUnit.SECONDS);
			System.out.println("OnlineInterfaceService: Web server is now online.");
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		if (authorized) {
			httpServer.stop();
			httpsServer.stop();
			executor.shutdownNow();
			try {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return super.stop();
	}
	
	@Override
	public void onSocketCreated(HttpSocket socket) {
		Log.i(TAG, "Received connection from: %s:%d  [%s]", socket.getInetAddress(), socket.getPort(), socket.isSecure() ? "secure" : "insecure");
	}
	
	@Override
	public void onRequestReceived(HttpSocket socket, HttpRequest request) {
		try {
			if (request.getType().equals("POST")) {
				String [] variables = request.getBody().split("&");
				if (variables.length == 2) {
					Map<String, String> varMap = new HashMap<>();
					for (String str : variables) {
						String [] var = str.split("=");
						if (var.length == 2)
							varMap.put(var[0], var[1]);
					}
					if (varMap.containsKey("username") && varMap.containsKey("password"))
						login(socket, varMap.get("username"), varMap.get("password"));
				}
			}
			if (!socket.isSecure()) {
				socket.redirect(new URL(request.getURI().getPath()).toString());
				return;
			}
			handler.handleRequest(socket, request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof PlayerEventIntent) {
			PlayerEventIntent pei = (PlayerEventIntent) i;
			switch (pei.getEvent()) {
				case PE_ZONE_IN:
					data.addOnlinePlayer(pei.getPlayer());
					break;
				case PE_LOGGED_OUT:
					data.removeOnlinePlayer(pei.getPlayer());
					break;
				default:
					break;
			}
		}
	}
	
	private void collectData() {
		data.updateResourceUsage();
	}
	
	private InetAddress getBindAddr(Config c, String firstTry, String secondTry) {
		String t = firstTry;
		try {
			if (c.containsKey(firstTry))
				return InetAddress.getByName(c.getString(firstTry, "127.0.0.1"));
			t = secondTry;
			if (c.containsKey(secondTry))
				return InetAddress.getByName(c.getString(secondTry, "127.0.0.1"));
		} catch (UnknownHostException e) {
			System.err.println("NetworkListenerService: Unknown host for IP: " + t);
		}
		return null;
	}
	
	private void login(HttpSocket socket, String username, String password) {
		HttpSession session = socket.getSession();
		if (session == null)
			return;
		synchronized (getUser) {
			try {
				getUser.setString(1, username);
				boolean prevAuthenticated = session.isAuthenticated();
				session.setAuthenticated(false);
				try (ResultSet cursor = getUser.executeQuery()) {
					session.setAuthenticated(cursor.next() && isUserValid(cursor, password));
					if (session.isAuthenticated()) {
						if (!prevAuthenticated)
							Log.i(TAG, "[%s] Successfully logged in to online interface", username);
					} else {
						Log.w(TAG, "[%s] Failed to login to online interface. Incorrect user/pass", username);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean isUserValid(ResultSet set, String password) throws SQLException {
		if (password.isEmpty())
			return false;
		if (set.getBoolean("banned"))
			return false;
		String psqlPass = set.getString("password");
		if (psqlPass.length() != 60 && !psqlPass.startsWith("$2"))
			return psqlPass.equals(password);
		password = BCrypt.hashpw(BCrypt.hashpw(password, psqlPass), psqlPass);
		return psqlPass.equals(password);
	}
	
}
