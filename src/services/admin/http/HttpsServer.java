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
			e.printStackTrace();
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
