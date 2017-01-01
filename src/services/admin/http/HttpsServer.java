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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import resources.server_info.Config;
import resources.server_info.Log;

public class HttpsServer extends HttpServer {
	
	private static final String SERVER_KEYSTORE = "server.keystore";
	
	private SSLContext sslContext;
	private SSLServerSocketFactory sslServerSocketFactory;
	
	public HttpsServer(InetAddress addr, int port) {
		super(addr, port, true);
	}
	
	public boolean initialize(Config config) {
		String pass = config.getString("HTTPS-KEYSTORE-PASSWORD", "");
		System.setProperty("javax.net.ssl.keyStore", SERVER_KEYSTORE);
		System.setProperty("javax.net.ssl.keyStorePassword", pass);
		try {
			sslContext = SSLContext.getInstance("TLSv1.2");
			KeyManager [] managers = createKeyManagers(pass.toCharArray());
			if (managers == null)
				return false;
		    sslContext.init(managers, null, null);
			sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			return true;
		} catch (Exception e) {
			Log.e(this, e);
			return false;
		}
	}
	
	protected String getThreadFactoryName() {
		return "HttpsServer-%d";
	}
	
	protected ServerSocket createSocket() throws IOException {
		try {
			return sslServerSocketFactory.createServerSocket(getBindPort(), 0, getBindAddress());
		} catch (IOException e) {
			Log.e("HttpsServer", "Failed to start HTTPS server!");
			Log.e("HttpsServer", e);
		}
		return null;
	}
	
	private KeyManager [] createKeyManagers(char [] password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("JKS");
	    InputStream ksIs = new FileInputStream(SERVER_KEYSTORE);
	    try {
	        ks.load(ksIs, password);
	    } catch (IOException e) {
	    	return null; // Password invalid
	    } finally {
            ksIs.close();
	    }
	    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	    kmf.init(ks, password);
	    return kmf.getKeyManagers();
	}
	
}
