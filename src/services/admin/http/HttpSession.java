package services.admin.http;

public class HttpSession {
	
	private final String token;
	private boolean authenticated;
	
	public HttpSession(String token) {
		this.token = token;
		this.authenticated = false;
	}
	
	public String getToken() {
		return token;
	}
	
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}
	
}
