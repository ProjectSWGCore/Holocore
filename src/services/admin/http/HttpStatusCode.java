package services.admin.http;

public enum HttpStatusCode {
	/** The request is OK (this is the standard response for successful HTTP requests) */
	OK					(200, "OK"),
	/** The request has been successfully processed, but is not returning any content */
	NO_CONTENT			(204, "No Content"),
	/** The requested page has moved to a new URL */
	MOVED_PERMANENTLY	(301, "Moved Permanently"),
	/** The requested page has moved temporarily to a new URL */
	FOUND				(302, "Found"),
	/** The requested page can be found under a different URL */
	SEE_OTHER			(303, "See Other"),
	/** The requested page has moved temporarily to a new URL */
	TEMPORARY_REDIRECT	(307, "Temporary Redirect"),
	/** The request cannot be fulfilled due to bad syntax */
	BAD_REQUEST			(400, "Bad Request"),
	/** The request was a legal request, but the server is refusing to respond to it.
	    For use when authentication is possible but has failed or not yet been provided */
	UNAUTHORIZED		(401, "Unauthorized"),
	/** The request was a legal request, but the server is refusing to respond to it */
	FORBIDDEN			(403, "Forbidden"),
	/** The requested page could not be found but may be available again in the future */
	NOT_FOUND			(404, "Not Found"),
	/** A request was made of a page using a request method not supported by that page */
	METHOD_NOT_ALLOWED	(405, "Method Not Allowed"),
	/** A generic error message, given when no more specific message is suitable */
	INTERNAL_ERROR		(500, "Internal Error"),
	/** The server is currently unavailable (overloaded or down) */
	SERVICE_UNAVAILABLE	(503, "");
	
	private int code;
	private String name;
	
	HttpStatusCode(int code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
}
