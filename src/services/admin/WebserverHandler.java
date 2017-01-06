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

import resources.Location;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.player.Player;
import resources.server_info.Log;
import services.admin.http.HttpImageType;
import services.admin.http.HttpSession;
import services.admin.http.HttpSocket;
import services.admin.http.HttpSocket.HttpRequest;
import services.admin.http.HttpStatusCode;

class WebserverHandler {
	
	private static final Charset ASCII = Charset.forName("ASCII");

	private static final String REQUIRED_PREFIX = new File("res/webserver").getAbsolutePath().replace('/', File.separatorChar);
	private static final String INDEX_PATH = new File("res/webserver/index.html".replace('/', File.separatorChar)).getAbsolutePath();
	private static final String AUTHENTICATED_PATH = new File("res/webserver/authenticated.html".replace('/', File.separatorChar)).getAbsolutePath();
	
	private final WebserverData data;
	private final Pattern variablePattern;
	
	public WebserverHandler(WebserverData data) {
		this.data = data;
		variablePattern = Pattern.compile("\\$\\{.+\\}"); // Looks for variables: ${VAR_NAME}
	}
	
	public void handleRequest(HttpSocket socket, HttpRequest request) throws IOException {
		String file = request.getURI().toASCIIString();
		Map<String, String> getVariables = new HashMap<>();
		if (file.contains("?")) {
			String vars = file.substring(file.indexOf('?')+1);
			String [] variables = vars.split("&");
			for (String var : variables) {
				String [] parts = var.split("=", 2);
				if (parts.length == 2)
					getVariables.put(parts[0], parts[1]);
			}
			file = file.substring(0, file.indexOf('?'));
		}
		if (file.contains("#"))
			file = file.substring(0, file.indexOf('#'));
		switch (file) {
			case "/memory_usage.png":
				if (socket.getSession().isAuthenticated())
					socket.send(createMemoryUsage(), HttpImageType.PNG);
				else
					socket.send(HttpStatusCode.NOT_FOUND, request.getURI() + " is not found!");
				break;
			default: {
				byte [] response = parseFile(socket.getSession(), file, getVariables);
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
		drawDataSet(g, Color.GREEN, " C:", image.getWidth(), image.getHeight(), graphHeight, x, data.getSystemCpuUsage());
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
	
	private byte [] parseFile(HttpSession session, String filepath, Map<String, String> getVariables) throws IOException {
		String type = getFileType(filepath);
		File file = verifyAndModifyFile(session, filepath, type);
		if (file == null)
			return null;
		if (type.equalsIgnoreCase("text/html"))
			return parseHtmlFile(file, getVariables).getBytes(ASCII);
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
	
	private File verifyAndModifyFile(HttpSession session, String filepath, String type) {
		File file = new File("res/webserver" + filepath);
		if (File.separatorChar != '/')
			file = new File(file.getAbsolutePath().replace('/', File.separatorChar));
		if (file.isDirectory())
			file = new File(file, "index.html");
		if (file.getAbsolutePath().equals(INDEX_PATH)) {
			if (session.isAuthenticated()) {
				file = new File(AUTHENTICATED_PATH);
			}
		} else if (!session.isAuthenticated() && !type.equals("text/css") && !type.equals("text/js") && !type.startsWith("image"))
			return null;
		if (!verifyPath(file)) {
			Log.e("WebserverHandler", "Cannot access %s - not a valid path", file);
			return null;
		}
		return file;
	}
	
	private boolean verifyPath(File file) {
		if (!file.getAbsolutePath().startsWith(REQUIRED_PREFIX))
			return false;
		if (!file.isFile())
			return false;
		return true;
	}
	
	private String parseHtmlFile(File file, Map<String, String> getVariables) throws IOException {
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
					line = line.replaceAll("\\$\\{"+var+"\\}", getVariable(var, getVariables));
					matcher.reset(line);
				}
				builder.append(line + System.lineSeparator());
			}
			return builder.toString();
		}
	}
	
	private String getVariable(String var, Map<String, String> getVariables) throws IOException {
		var = var.toLowerCase(Locale.US);
		switch (var) {
			case "log":
				return data.getLog().replace("\n", "\n<br />");
			case "online_player_count": 
				return getOnlinePlayerCount();						
			case "online_players": 
				return getOnlinePlayerData();
			case "character_info": {
				if (!getVariables.containsKey("character_id"))
					return "";
				long i = Long.parseLong(getVariables.get("character_id"));
				for (Player p : data.getOnlinePlayers()) {
					if (p.getCreatureObject().getObjectId() == i) {
						return getCreatureData(p.getCreatureObject());
					}
				}
				return "No Creature Found!";
			}
			default:
				return "";
		}
	}
	
	private String getOnlinePlayerCount() {
		Set<Player> players = data.getOnlinePlayers();
		return "Online Players: [" + players.size() + "]";
	}
	
	private String getOnlinePlayerData() {
		Set<Player> players = data.getOnlinePlayers();
		StringBuilder ret = new StringBuilder("<table class=\"online_players_table\"><tr><th>Username</th><th>User ID</th><th>Character</th><th>Character ID</th></tr>");
		for (Player p : players) {
			ret.append("<tr>");
			long id = p.getCreatureObject().getObjectId();
			ret.append(String.format("<td class=\"online_player_cell\"><a href=\"?character_id=%d\">%s</a></td>", id, p.getUsername()));
			ret.append(String.format("<td class=\"online_player_cell\"><a href=\"?character_id=%d\">%d</a></td>", id, p.getUserId()));
			ret.append(String.format("<td class=\"online_player_cell\"><a href=\"?character_id=%d\">%s</a></td>", id, p.getCharacterName()));
			ret.append(String.format("<td class=\"online_player_cell\"><a href=\"?character_id=%d\">%d</a></td>", id, p.getCreatureObject().getObjectId()));
			ret.append("</tr>");
		}
		ret.append("</table>");
		return ret.toString();
	}
	
	private String getCreatureData(SWGObject creature) {
		StringBuilder str = new StringBuilder("");
		Location world = creature.getWorldLocation();
		str.append(String.format("  Object ID: %s<br />", creature.getObjectId()));
		str.append(String.format("       Name: %s<br />", creature.getObjectName()));
		str.append(String.format("<b>World:</b><br />"));
		str.append(addLocationData(world));
		if (creature.getParent() != null) {
			Location cell = creature.getLocation();
			str.append(String.format("<b>Local:</b><br />"));
			str.append(addLocationData(cell));
			SWGObject parent = creature.getParent();
			if (parent != null) {
				str.append(String.format("    <b>Parent:</b><br />"));
				str.append(addParentData(parent, "    "));
				parent = parent.getParent();
				if (parent != null) {
					str.append(String.format("        <b>Grandparent:</b><br />"));
					str.append(addParentData(parent, "        "));
				}
			}
		}
		return str.toString().replace(" ", "&nbsp;").replace("<br&nbsp;/>", "<br />");
	}
	
	private String addParentData(SWGObject obj, String indent) {
		StringBuilder str = new StringBuilder("");
		str.append(indent + String.format("  Object ID: %d<br />", obj.getObjectId()));
		str.append(indent + String.format("   Template: %s<br />", obj.getTemplate()));
		if (obj instanceof CellObject) {
			str.append(indent + String.format(" Cell Index: %d<br />", ((CellObject) obj).getNumber()));
			str.append(indent + String.format("  Cell Name: %s<br />", ((CellObject) obj).getCellName()));
		}
		return str.toString();
	}
	
	private String addLocationData(Location l) {
		StringBuilder str = new StringBuilder("");
		str.append(String.format("   Location: %.2f, %.2f, %.2f [%s]<br />", l.getX(), l.getY(), l.getZ(), l.getTerrain()));
		str.append(String.format("Orientation: %.2f, %.2f, %.2f, %.2f<br />", l.getOrientationX(), l.getOrientationY(), l.getOrientationZ(), l.getOrientationW()));
		return str.toString();
	}
	
}
