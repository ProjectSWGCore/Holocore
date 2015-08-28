package services.admin.http;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import resources.server_info.Log;
import services.admin.http.HttpCookie.CookieFlag;

public class HttpSocket implements Closeable {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final Socket socket;
	private final BufferedReader reader;
	private final BufferedWriter writer;
	private final OutputStream rawOutputStream;
	private final boolean secure;
	private HttpSession session;
	
	public HttpSocket(Socket socket, boolean secure) {
		this.socket = socket;
		this.reader = createBufferedReader();
		this.writer = createBufferedWriter();
		this.rawOutputStream = getOutputStream();
		this.secure = secure;
		this.session = null;
	}
	
	/**
	 * Waits for the next HTTP request to come in, then returns the request
	 * sent by the client - or null if the client closes the connection.
	 * @return the HttpRequest sent by the client, or null if the connection is
	 * closed
	 */
	public HttpRequest waitForRequest() {
		while (!socket.isClosed()) {
			String line = readLine();
			boolean hasData = line != null && !line.isEmpty();
			String [] req = null;
			Map<String, String> params = new HashMap<>();
			Set<HttpCookie> cookies = new HashSet<>();
			StringBuilder body = new StringBuilder("");
			while (line != null && !line.isEmpty()) {
				if (req == null)
					req = line.split(" ", 3);
				String [] parts = line.split(": ", 2);
				if (parts.length == 2) {
					if (parts[0].equals("Cookie")) {
						cookies.addAll(Arrays.asList(HttpCookie.decodeCookies(parts[1])));
					} else {
						params.put(parts[0], parts[1]);
					}
				}
				hasData = !line.isEmpty();
				line = readLine();
			}
			if (params.containsKey("Content-Length")) {
				Integer i = Integer.valueOf(params.get("Content-Length"));
				if (i.intValue() > 0) {
					String bodyStr = readBytes(i.intValue());
					if (bodyStr == null)
						Log.e("HttpSocket", "Failed to read data in %s request: %s - returned null", req.length==0?"null":req[0], req.length<2?"null":req[1]);
					else {
						if (bodyStr.length() != i.intValue())
							Log.w("HttpSocket", "Failed to read all data in %s request: %s", req.length==0?"null":req[0], req.length<2?"null":req[1]);
						body.append(bodyStr);
					}
				}
			}
			if (hasData) {
				String type = null;
				URI uri = null;
				String version = null;
				if (req.length >= 1)
					type = req[0];
				if (req.length >= 2)
					uri = URI.create(req[1]);
				if (req.length >= 3)
					version = req[2];
				return new HttpRequest(type, uri, version, params, cookies, body.toString());
			}
			if (line == null)
				break;
		}
		return null;
	}
	
	public void setSession(HttpSession session) {
		this.session = session;
	}
	
	public HttpSession getSession() {
		return session;
	}
	
	public void redirect(String url) throws IOException {
		Map<String, String> params = new HashMap<>();
		params.put("Location", url);
		send(HttpStatusCode.MOVED_PERMANENTLY, params, new HashSet<>(), "text/html", "");
	}
	
	public void send(HttpStatusCode code) throws IOException {
		send(code, "");
	}
	
	public void send(String response) throws IOException {
		send(HttpStatusCode.OK, response);
	}
	
	public void send(String contentType, byte [] response) throws IOException {
		send(HttpStatusCode.OK, new HashMap<>(), new HashSet<>(), contentType, response);
	}
	
	public void send(BufferedImage image, HttpImageType type) throws IOException {
		String contentType = "image";
		switch (type) {
			case GIF:
				contentType = "image/gif";
				break;
			case JPG:
				contentType = "image/jpeg";
				break;
			case PNG:
				contentType = "image/png";
				break;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 3);
		if (!ImageIO.write(image, type.name().toLowerCase(Locale.US), baos))
			throw new IllegalArgumentException("Cannot write image with type: " + type);
		send(HttpStatusCode.OK, new HashMap<>(), new HashSet<>(), contentType, baos.toByteArray());
	}
	
	public void send(HttpStatusCode code, String response) throws IOException {
		if (code == HttpStatusCode.OK && response.isEmpty())
			code = HttpStatusCode.NO_CONTENT;
		send(code, new HashMap<>(), new HashSet<>(), "text/html", response);
	}
	
	private void send(HttpStatusCode code, Map<String, String> params, Set<HttpCookie> cookies, String contentType, String response) throws IOException {
		params.put("Content-Length", Integer.toString(response.length()));
		params.put("Content-Type", contentType);
		sendHeader(code, params, cookies);
		writer.write(response);
		writer.flush();
	}
	
	private void send(HttpStatusCode code, Map<String, String> params, Set<HttpCookie> cookies, String contentType, byte [] data) throws IOException {
		params.put("Content-Length", Integer.toString(data.length));
		params.put("Content-Type", contentType);
		sendHeader(code, params, cookies);
		writer.flush();
		rawOutputStream.write(data);
		rawOutputStream.flush();
	}
	
	private void sendHeader(HttpStatusCode code, Map<String, String> params, Set<HttpCookie> cookies) throws IOException {
		if (session != null)
			cookies.add(new HttpCookie("sessionToken", session.getToken(), CookieFlag.SECURE, CookieFlag.HTTP_ONLY));
		write("HTTP/1.1 %d %s", code.getCode(), code.getName());
		for (Entry<String, String> param : params.entrySet())
			write(param.getKey() + ": " + param.getValue());
		for (HttpCookie cookie : cookies)
			write("Set-Cookie: " + cookie.encode());
		writer.newLine();
	}
	
	private String readLine() {
		try {
			return reader.readLine();
		} catch (Exception e) {
			return null;
		}
	}
	
	private String readBytes(int length) {
		try {
			if (length == 0)
				return "";
			char [] data = new char[length];
			int read = reader.read(data);
			if (read == -1)
				return null;
			return new String(data, 0, read);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
	public boolean isOpened() {
		return reader != null && writer != null;
	}
	
	public boolean isClosed() {
		return socket.isClosed();
	}
	
	public boolean isSecure() {
		return secure;
	}
	
	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
	
	public int getPort() {
		return socket.getPort();
	}
	
	private void write(String str, Object ... args) throws IOException {
		writer.write(String.format(str, args) + System.lineSeparator());
	}
	
	private BufferedReader createBufferedReader() {
		try {
			return new BufferedReader(new InputStreamReader(socket.getInputStream(), ASCII));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private BufferedWriter createBufferedWriter() {
		try {
			return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), ASCII));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private OutputStream getOutputStream() {
		try {
			return socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static class HttpRequest {
		private final String type;
		private final URI uri;
		private final String httpVersion;
		private final Map<String, String> params;
		private final Set<HttpCookie> cookies;
		private final String body;
		
		private HttpRequest(String requestType, URI uri, String httpVersion, Map<String, String> params, Set<HttpCookie> cookies, String body) {
			this.type = requestType;
			this.uri = uri;
			this.httpVersion = httpVersion;
			this.params = params;
			this.cookies = cookies;
			this.body = body;
		}
		
		/**
		 * Gets the type of HTTP request sent. It could be one of: GET, POST,
		 * HEAD.
		 * @return the type of HTTP request
		 */
		public String getType() {
			return type;
		}
		
		/**
		 * Gets the requested URI sent
		 * @return the requested URI
		 */
		public URI getURI() {
			return uri;
		}
		
		/**
		 * Gets the HTTP version of the client, typical format is: HTTP/1.1
		 * @return the HTTP version of the client
		 */
		public String getHttpVersion() {
			return httpVersion;
		}
		
		/**
		 * Gets all the paramters sent in the HTTP request, such as:
		 * "User-Agent", "Accept", etc.
		 * @return the HTTP request parameters
		 */
		public Map<String, String> getParams() {
			return Collections.unmodifiableMap(params);
		}
		
		/**
		 * Gets all the cookies sent in the HTTP request
		 * @return the cookies
		 */
		public Set<HttpCookie> getCookies() {
			return Collections.unmodifiableSet(cookies);
		}
		
		/**
		 * Gets the request body
		 * @return the request body
		 */
		public String getBody() {
			return body;
		}
	}
	
}
