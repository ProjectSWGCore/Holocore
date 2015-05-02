package network.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

public class MD5 {
	
	public static String digest(String text) {
		String result = text;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			result = convertToHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private static String convertToHex(byte[] b) {
		StringBuilder result = new StringBuilder(32);
		for (int i = 0; i < b.length; i++) {
			result.append(Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ));
		}
		return result.toString();
	}
}