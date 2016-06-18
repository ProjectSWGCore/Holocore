/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.admin;

import intents.PlayerEventIntent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
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
			Log.e(this, "Failed to initialize HTTPS server! Incorrect password?");
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
			Log.i(this, "Web server is now online.");
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
				try {
					socket.redirect(new URL(request.getURI().getPath()).toString());
				} catch (MalformedURLException e) {
					Log.w(this, "Malformed URL: " + request.getURI().getPath());
				}
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
				case PE_ZONE_IN_CLIENT:
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
			Log.e(this, "Unknown host for IP: " + t);
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
