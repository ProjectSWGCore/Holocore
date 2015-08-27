package services.admin;

import intents.PlayerEventIntent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.server_info.Config;
import resources.server_info.Log;
import services.admin.http.HttpServer;
import services.admin.http.HttpServer.HttpServerCallback;
import services.admin.http.HttpSocket;
import services.admin.http.HttpSocket.HttpRequest;
import services.admin.http.HttpStatusCode;
import services.admin.http.HttpsServer;
import utilities.ThreadUtilities;

public class OnlineInterfaceService extends Service implements HttpServerCallback {
	
	private static final String TAG = "OnlineInterfaceService";
	
	private final WebserverData data;
	private final WebserverHandler handler;
	private final Runnable dataCollectionRunnable;
	private ScheduledExecutorService executor;
	private HttpsServer httpsServer;
	private HttpServer httpServer;
	private boolean authorized;
	
	public OnlineInterfaceService() {
		data = new WebserverData();
		handler = new WebserverHandler(data);
		dataCollectionRunnable = () -> collectData();
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
			if (!request.getType().equals("GET")) {
				socket.send(HttpStatusCode.METHOD_NOT_ALLOWED);
				return;
			}
			if (!socket.isSecure()) {
				socket.redirect(new URL("https", httpsServer.getBindAddress().getHostName(), httpsServer.getBindPort(), request.getURI().getPath()).toString());
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
	
}
