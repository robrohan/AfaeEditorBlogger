package afaeeditorblogger.internal;

/**
 * This is a wrapper class for base64 data so when it's sent via xmlrpc 
 * it can have it's own tag
 * @author robrohan
 *
 */
public class Base64Data {
	private String storage = "";
	
	public void setStorage(String data) {
		this.storage = data;
	}
	
	public String getStorage() {
		return storage;
	}
	
	public String toString() {
		return getStorage();
	}
}
