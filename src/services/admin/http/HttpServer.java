package services.admin.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import resources.server_info.Log;
import services.admin.http.HttpSocket.HttpRequest;
import utilities.ThreadUtilities;

public class HttpServer {
	
	private static final String TAG = "HttpServer";
	
	private final InetAddress addr;
	private final int port;
	private final Map<String, HttpSession> sessions;
	
	private ExecutorService executor;
	private ServerSocket serverSocket;
	private AtomicInteger currentConnections;
	private HttpServerCallback callback;
	private int maxConnections;
	private boolean secure;
	
	protected HttpServer(InetAddress addr, int port, boolean secure) {
		this.addr = addr;
		this.port = port;
		this.sessions = new HashMap<>();
		this.currentConnections = new AtomicInteger(0);
		this.callback = null;
		this.maxConnections = 2;
		this.secure = secure;
	}
	
	public HttpServer(InetAddress addr, int port) {
		this(addr, port, false);
	}
	
	public void start() {
		executor = Executors.newCachedThreadPool(ThreadUtilities.newThreadFactory(getThreadFactoryName()));
		try {
			serverSocket = createSocket();
			startAcceptThread(serverSocket, secure);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setServerCallback(HttpServerCallback callback) {
		this.callback = callback;
	}
	
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
	
	public final InetAddress getBindAddress() {
		return addr;
	}
	
	public final int getBindPort() {
		return port;
	}
	
	protected String getThreadFactoryName() {
		return "HttpServer-%d";
	}
	
	protected ServerSocket createSocket() throws IOException {
		return new ServerSocket(port, 0, addr);
	}
	
	private void startAcceptThread(ServerSocket serverSocket, boolean secure) {
		executor.submit(() -> {
			try {
				acceptThread(serverSocket, secure);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
	}
	
	private void startSocketThread(HttpSocket socket) {
		executor.submit(() -> {
			try {
				socketThread(socket);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (currentConnections) {
				currentConnections.decrementAndGet();
			}
		});
	}
	
	private void acceptThread(ServerSocket serverSocket, boolean secure) {
		Log.i(TAG, "Now listening over HTTP%s. Address: %s:%d", secure?"S":"", serverSocket.getInetAddress(), serverSocket.getLocalPort());
		while (serverSocket.isBound() && !serverSocket.isClosed()) {
			try {
				Socket socket = serverSocket.accept();
				HttpSocket httpSocket = new HttpSocket(socket, secure);
				if (!httpSocket.isOpened()) {
					httpSocket.close();
					continue;
				}
				if (currentConnections.get() >= maxConnections) {
					httpSocket.send(HttpStatusCode.SERVICE_UNAVAILABLE, "The server has reached it's maximum connection limit: " + maxConnections);
					socket.close();
				} else {
					if (currentConnections.incrementAndGet() > maxConnections) {
						socket.close();
						currentConnections.decrementAndGet();
					} else {
						startSocketThread(httpSocket);
					}
				}
			} catch (SocketException e) {
				if (serverSocket.isClosed())
					break;
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		Log.i(TAG, "No longer listening over HTTP%s!", secure?"S":"");
	}
	
	private void socketThread(HttpSocket socket) throws IOException {
		onSocketCreated(socket);
		HttpRequest request;
		while (socket.isOpened() && !socket.isClosed()) {
			request = socket.waitForRequest();
			if (request == null)
				break;
			if (request.getType() == null || request.getURI() == null || request.getHttpVersion() == null) {
				socket.send(HttpStatusCode.BAD_REQUEST);
				continue;
			}
			onRequestReceived(socket, request);
		}
	}
	
	private HttpSession getSessionForRequest(HttpRequest request) {
		String token = null;
		for (HttpCookie cookie : request.getCookies()) {
			if (cookie.getKey() != null && cookie.getKey().equals("sessionToken")) {
				if (sessions.containsKey(cookie.getValue()))
					token = cookie.getValue();
			}
		}
		if (token == null)
			token = generateSessionToken();
		HttpSession session = null;
		if (!sessions.containsKey(token)) {
			session = new HttpSession(token);
			sessions.put(token, session);
		} else
			session = sessions.get(token);
		return session;
	}
	
	private String generateSessionToken() {
		Random r = new Random();
		StringBuilder builder = new StringBuilder(24);
		int rand;
		for (int i = 0; i < 24; i++) {
			rand = r.nextInt(26*2+10);
			if (rand < 26)
				builder.append((char) ('A' + rand));
			else if (rand < 52)
				builder.append((char) ('a' + (rand-26)));
			else
				builder.append((char) ('0' + (rand-52)));
		}
		return builder.toString();
	}
	
	private void onSocketCreated(HttpSocket socket) {
		if (callback == null)
			return;
		try {
			callback.onSocketCreated(socket);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void onRequestReceived(HttpSocket socket, HttpRequest request) {
		if (callback == null)
			return;
		try {
			socket.setSession(getSessionForRequest(request));
			callback.onRequestReceived(socket, request);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public interface HttpServerCallback {
		void onSocketCreated(HttpSocket socket);
		void onRequestReceived(HttpSocket socket, HttpRequest request);
	}
	
}
