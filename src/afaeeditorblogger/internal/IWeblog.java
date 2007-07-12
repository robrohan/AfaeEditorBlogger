package afaeeditorblogger.internal;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.core.runtime.CoreException;

public interface IWeblog {
	
	public static String ITEM_ID = "ID";
	public static String ITEM_TITLE = "Title";
	public static String ITEM_CATEGORIES = "Categories";
	public static String ITEM_DATE = "Created";
	public static String ITEM_DESCRIPTION = "Description";
	
	public static String ITEM_ALLOW_COMMENTS = "Comments";
	public static String ITEM_ALLOW_PINGS = "Pings";
	
	/**
	 * Gets a single web post entry. The post will be put into
	 * a new document (or, if one exists already) it will open it, and
	 * put the text into the editor.
	 * @param id
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 * @throws CoreException
	 * @throws IOException
	 */
	public void getPost(String id) throws MalformedURLException, XmlRpcException, CoreException, IOException;
	
	/**
	 * Gets a list of the most reason posts
	 * @return
	 * @throws XmlRpcException
	 * @throws MalformedURLException
	 */
	public Object[] getRecentPosts() throws XmlRpcException, MalformedURLException;
	
	/**
	 * Post the current document as an entry to the configured Blog
	 * @return
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 */
	public String postEntry();
	
	public String postMedia(String name, String type, Base64Data bits);
	
}
