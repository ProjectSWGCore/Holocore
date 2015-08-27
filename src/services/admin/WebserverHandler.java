package services.admin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import resources.player.Player;
import services.admin.http.HttpImageType;
import services.admin.http.HttpSocket;
import services.admin.http.HttpSocket.HttpRequest;
import services.admin.http.HttpStatusCode;

class WebserverHandler {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final WebserverData data;
	private final Pattern variablePattern;
	
	public WebserverHandler(WebserverData data) {
		this.data = data;
		variablePattern = Pattern.compile("\\$\\{.+\\}"); // Looks for variables: ${VAR_NAME}
	}
	
	public void handleRequest(HttpSocket socket, HttpRequest request) throws IOException {
		String file = request.getURI().toASCIIString();
		if (file.contains("?"))
			file = file.substring(0, file.indexOf('?'));
		switch (file) {
			case "/memory_usage.png":
				socket.send(createMemoryUsage(), HttpImageType.PNG);
				break;
			default: {
				byte [] response = parseFile(file);
				if (response == null)
					socket.send(HttpStatusCode.NOT_FOUND, request.getURI() + " is not found!");
				else
					socket.send(getFileType(file), response);
				break;
			}
		}
	}
	
	private BufferedImage createMemoryUsage() {
		double [] memoryUsage = data.getMemoryUsage();
		final int graphHeight = 300;
		BufferedImage image = new BufferedImage(900, graphHeight + 25, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = image.createGraphics();
		Font font = g.getFont();
		g.setFont(font.deriveFont(Font.TRUETYPE_FONT, Math.min(image.getHeight()/10.0f, 25.0f)));
		Map <RenderingHints.Key, Object> hints = new HashMap<>();
		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHints(new RenderingHints(hints));
		g.setColor(Color.WHITE);
		renderGraph(g, image.getWidth(), image.getHeight(), graphHeight);
		FontMetrics fm = g.getFontMetrics();
		String inUse = getMemoryInUse();
		int width = fm.stringWidth(inUse);
		int x = 5;
		x += drawStr(g, image.getHeight(), x, "Proc:");
		g.drawString(inUse, image.getWidth()-width - 5, image.getHeight() - 5);
		x += drawDataSet(g, Color.RED, " M:", image.getWidth(), image.getHeight(), graphHeight, x, memoryUsage);
		x += drawDataSet(g, Color.CYAN, " C:", image.getWidth(), image.getHeight(), graphHeight, x, data.getCpuUsage());
		g.setColor(Color.WHITE);
		x += drawStr(g, image.getHeight(), x, "  Sys:");
		x += drawDataSet(g, Color.YELLOW, " M:", image.getWidth(), image.getHeight(), graphHeight, x, data.getSystemMemoryUsage());
		x += drawDataSet(g, Color.GREEN, " C:", image.getWidth(), image.getHeight(), graphHeight, x, data.getSystemCpuUsage());
		return image;
	}
	
	private void renderGraph(Graphics2D g, int width, int height, int graphHeight) {
		g.drawRect(0, 0, width-1, graphHeight-1);
		g.drawLine(0, height-1, width, height-1);
		for (int i = 1; i < 10; i++) {
			g.drawLine(0, (int) ((i/10.0)*graphHeight), 10, (int) ((i/10.0)*graphHeight));
		}
	}
	
	private int drawDataSet(Graphics2D g, Color c, String pre, int width, int height, int graphHeight, int x, double [] data) {
		g.setColor(c);
		drawLines(g, data, width, graphHeight-1);
		return drawStr(g, height, x, String.format("%s%.2f%%", pre, (int) (data[data.length-1] * 10000) / 100.0));
	}
	
	private int drawStr(Graphics2D g, int height, int x, String str) {
		g.drawString(str, x, height - 5);
		return g.getFontMetrics().stringWidth(str);
	}
	
	private void drawLines(Graphics2D g, double [] data, int width, int height) {
		int start = 0;
		for (double d : data) {
			if (d == 0)
				start++;
			else
				break;
		}
		int prevX = -1;
		int prevY = -1;
		for (int i = start; i < data.length; i++) {
			int x = (int) ((double) i / data.length * width);
			int y = (int) ((1-data[i]) * height);
			if (prevX != -1 && prevY != -1)
				g.drawLine(prevX, prevY, x, y);
			prevX = x;
			prevY = y;
		}
	}
	
	private String getMemoryInUse() {
		double memory = Runtime.getRuntime().totalMemory();
		String [] types = new String[]{"B", "KB", "MB", "GB"};
		String type = types[0];
		for (int i = 0; i < types.length && memory >= 1024; i++) {
			memory /= 1024;
			type = types[i];
		}
		return String.format("In-Use: %.2f%s", memory, type);
	}
	
	private String getFileType(String filepath) {
		if (!filepath.contains("."))
			return "text/html";
		String type = filepath.substring(filepath.lastIndexOf('.')+1).toLowerCase(Locale.US);
		switch (type) {
			case "png":
			case "gif":
			case "jpg":
			case "jpeg":
			case "ico":
				return "image/" + type;
			default:
				return "text/" + type;
		}
	}
	
	private byte [] parseFile(String filepath) throws IOException {
		File file = new File("res/webserver" + filepath);
		if (file.isDirectory())
			file = new File(file, "index.html");
		String type = getFileType(filepath);
		if (!verifyPath(file))
			return null;
		if (type.equalsIgnoreCase("text/html"))
			return parseHtmlFile(file).getBytes(ASCII);
		try (InputStream is = new FileInputStream(file)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte [] buffer = new byte[Math.min(1024, is.available())];
			while (is.available() > 0) {
				if (is.available() > buffer.length && buffer.length < 1024)
					buffer = new byte[Math.min(1024, is.available())];
				int len = is.read(buffer);
				baos.write(buffer, 0, len);
			}
			return baos.toByteArray();
		}
	}
	
	private boolean verifyPath(File file) {
		File parent = new File("res/webserver");
		if (!file.getAbsolutePath().startsWith(parent.getAbsolutePath()))
			return false;
		if (!file.isFile())
			return false;
		return true;
	}
	
	private String parseHtmlFile(File file) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ASCII))) {
			String line = null;
			StringBuilder builder = new StringBuilder();
			while (reader.ready()) {
				line = reader.readLine();
				if (line == null)
					break;
				Matcher matcher = variablePattern.matcher(line);
				while (matcher.find()) {
					String var = matcher.group();
					var = var.substring(2, var.length()-1);
					line = line.replaceAll("\\$\\{"+var+"\\}", getVariable(var));
					matcher.reset(line);
				}
				builder.append(line + System.lineSeparator());
			}
			return builder.toString();
		}
	}
	
	private String getVariable(String var) throws IOException {
		var = var.toLowerCase(Locale.US);
		switch (var) {
			case "log":
				return data.getLog().replace("\n", "\n<br />");
			case "online_players": {
				Set<Player> players = data.getOnlinePlayers();
				StringBuilder ret = new StringBuilder("Online Players: ["+players.size()+"]<br />");
				ret.append("<table class=\"online_players_table\"><tr><th>Username</th><th>User ID</th><th>Character</th><th>Character ID</th></tr>");
				for (Player p : players) {
					ret.append("<tr>");
					ret.append(String.format("<td class=\"online_player_cell\">%s</td>", p.getUsername()));
					ret.append(String.format("<td class=\"online_player_cell\">%d</td>", p.getUserId()));
					ret.append(String.format("<td class=\"online_player_cell\">%s</td>", p.getCharacterName()));
					ret.append(String.format("<td class=\"online_player_cell\">%d</td>", p.getCreatureObject().getObjectId()));
					ret.append("</tr>");
				}
				ret.append("</table>");
				return ret.toString();
			}
			default:
				return "";
		}
	}
	
}
