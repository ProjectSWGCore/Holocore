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
package services.admin.http;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class HttpCookie implements Comparable<HttpCookie> {
	
	private static final String EXPIRES_FORMAT = "EEE, dd-MM-yyyy HH:mm:ss zzz"; // Wed, 18-05-2015 16:00:00 GMT
	
	private String key;
	private String value;
	private String domain;
	private String path;
	private long expires;
	private Set<CookieFlag> flags;
	
	public HttpCookie(String value, CookieFlag ... flags) {
		if (value == null)
			throw new IllegalArgumentException("Value for cookie cannot be null!");
		this.key = null;
		this.value = value;
		if (flags.length == 0)
			this.flags = EnumSet.noneOf(CookieFlag.class);
		else
			this.flags = EnumSet.copyOf(Arrays.asList(flags));
		this.domain = null;
		this.path = null;
		this.expires = 0;
	}
	
	public HttpCookie(String key, String value, CookieFlag ... flags) {
		this(value, flags);
		this.key = key;
	}
	
	public HttpCookie(String key, String value, long expires, CookieFlag ... flags) {
		this(key, value, flags);
		this.expires = expires;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getPath() {
		return path;
	}
	
	public long getExpires() {
		return expires;
	}
	
	public Set<CookieFlag> getFlags() {
		return Collections.unmodifiableSet(flags);
	}
	
	public boolean hasFlag(CookieFlag flag) {
		return flags.contains(flag);
	}
	
	public boolean hasExpired() {
		return expires != 0 && System.currentTimeMillis() >= expires;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setExpires(long expires) {
		this.expires = expires;
	}
	
	public void addFlag(CookieFlag flag) {
		flags.add(flag);
	}
	
	public void removeFlag(CookieFlag flag) {
		flags.remove(flag);
	}
	
	@Override
	public int compareTo(HttpCookie c) {
		if (key != null && c.key == null)
			return 1;
		if (key == null && c.key != null)
			return -1;
		if (!value.equals(c.value))
			return value.compareTo(c.value);
		if (expires < c.expires)
			return -1;
		if (expires > c.expires)
			return 1;
		return 0;
	}
	
	@Override
	public int hashCode() {
		int code = 0;
		if (key != null)
			code += key.hashCode();
		code ^= value.hashCode();
		if (expires != 0)
			code ^= Long.valueOf(expires).hashCode();
		return code;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof HttpCookie))
			return false;
		HttpCookie c = (HttpCookie) obj;
		if (key != null && c.key == null)
			return false;
		if (c.key != null && key == null)
			return false;
		if (!c.value.equals(value))
			return false;
		return c.expires == expires;
	}
	
	public String encode() {
		StringBuilder str = new StringBuilder("");
		if (key == null)
			str.append(value);
		else
			str.append(encodeCookieString(key) + "=" + encodeCookieString(value));
		if (expires != 0)
			str.append("; expires=" + encodeCookieString(new SimpleDateFormat(EXPIRES_FORMAT).format(expires)));
		if (domain != null)
			str.append("; domain=" + encodeCookieString(domain));
		if (path != null)
			str.append("; path=" + encodeCookieString(path));
		for (CookieFlag flag : flags)
			str.append("; " + flag.encode());
		return str.toString();
	}
	
	private String encodeCookieString(String str) {
		return str.replace(" ", "%20").replace("\t", "%09").replace("\n", "%0A").replace("&", "%26").replace(";", "%3B");
	}
	
	public static HttpCookie [] decodeCookies(String str) {
		String [] parts = str.split("; ");
		HttpCookie [] cookies = new HttpCookie[parts.length];
		for (int i = 0; i < parts.length; i++) {
			cookies[i] = decodeCookie(decodeCookieString(parts[i]));
		}
		return cookies;
	}
	
	private static HttpCookie decodeCookie(String str) {
		String [] parts = str.split("=", 2);
		if (parts.length == 0)
			return new HttpCookie("");
		if (parts.length == 1)
			return new HttpCookie(parts[0]);
		return new HttpCookie(parts[0], parts[1]);
	}
	
	private static String decodeCookieString(String str) {
		return str.replace("%20", " ").replace("%09", "\t").replace("%0A", "\n").replace("%26", "&").replace("%3B", ";");
	}
	
	public static enum CookieFlag {
		/**
		 * Security feature where this cookie is only sent from the client
		 * over a secure HTTPS connection.
		 */
		SECURE		("secure"),
		/**
		 * Security feature to avoid Cross Site Scripting (XSS), where this
		 * cookie is unable to be accessed through javascript - only via HTTP.
		 */
		HTTP_ONLY	("HttpOnly");
		
		private String encoded;
		
		CookieFlag(String encoded) {
			this.encoded = encoded;
		}
		
		protected String encode() {
			return encoded;
		}
	}
	
}
