package afaeeditorblogger.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import afaeeditorblogger.Activator;
import afaeeditorblogger.preferences.PreferenceConstants;

public class MetaWeblog implements IWeblog {
	protected IPreferenceStore store;
	
	protected static HashMap<String, String> translations = new HashMap<String, String>();
	protected static HashMap<String, String> translations_reverse = new HashMap<String, String>();
	static {
		//tables used to translate header inforamtion to and from client readable -> blog setting
		translations.put("UserID", "userid");
		translations.put(ITEM_DATE, "dateCreated");
		translations.put(ITEM_ID, "postid");
		translations.put(ITEM_DESCRIPTION, "description");
		translations.put(ITEM_TITLE, "title");
		translations.put("Link", "link");
		translations.put("PermaLink", "permaLink");
		translations.put("Excerpt", "mt_excerpt");
		translations.put(ITEM_ALLOW_COMMENTS, "mt_allow_comments");
		translations.put(ITEM_ALLOW_PINGS, "mt_allow_pings");
		translations.put("ConvertBreaks", "mt_convert_breaks");
		translations.put("Keywords", "mt_keywords");
		translations.put(ITEM_CATEGORIES, "categories");
		
		//kinda lame, but the translation the other way...
		translations_reverse.put("userid","UserID");
		translations_reverse.put("dateCreated", ITEM_DATE);
		translations_reverse.put("postid", ITEM_ID);
		translations_reverse.put("description", ITEM_DESCRIPTION);
		translations_reverse.put("title",ITEM_TITLE);
		translations_reverse.put("link","Link");
		translations_reverse.put("permaLink","PermaLink");
		translations_reverse.put("mt_excerpt", "Excerpt");
		translations_reverse.put("mt_allow_comments", ITEM_ALLOW_COMMENTS);
		translations_reverse.put("mt_allow_pings",ITEM_ALLOW_PINGS);
		translations_reverse.put("mt_convert_breaks", "ConvertBreaks");
		translations_reverse.put("mt_keywords", "Keywords");
		translations_reverse.put("categories",ITEM_CATEGORIES);
	}
	
	/////////////////////////////////////////////////////////////////////
	// To listen for posting Events
	static ArrayList<IPropertyChangeListener> myListeners;
	
	// A public method that allows listener registration
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if(!myListeners.contains(listener))
			myListeners.add(listener);
	}

	// A public method that allows listener registration
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		myListeners.remove(listener);
	}

	void notifyPost() {
		// Code to Invoke Web Service Periodically, and retrieve information		
		// Post Invocation, inform listeners
		for (Iterator iter = myListeners.iterator(); iter.hasNext();) {
			IPropertyChangeListener element = (IPropertyChangeListener) iter.next();
			element.propertyChange(
				new PropertyChangeEvent(this, "BlogPostingEvent" , null , "")
			);
		}
	}
	/////////////////////////////////////////////////////////////////////
		
	public MetaWeblog() {
		if(myListeners == null){
			myListeners = new ArrayList<IPropertyChangeListener>();
			Collections.synchronizedList(myListeners);
		}
		
		store = Activator.getDefault().getPreferenceStore();
	}
	
	/**
	 * Creates and configures an instance of XmlRpcClinet that can be used to 
	 * communicate with an RPC service
	 * @return an XmlRpcClient
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 */
	private XmlRpcClient createClient() throws MalformedURLException, XmlRpcException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(store.getString(PreferenceConstants.P_BLOGGING_URL)));
		config.setEncoding("UTF-8");
		
		//System.out.println("Getting from: " + store.getString(PreferenceConstants.P_BLOGGING_URL));
		//config.getXmlRpcServer().getTypeConverterFactory()
		
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		client.setTypeFactory(new BloggingTypeFactory(client));
		
		return client;
	}
	
	public void getPost(String id) throws MalformedURLException, XmlRpcException, CoreException, IOException {
		XmlRpcClient client = createClient();
		
		//bit of a hack here to support Wordpress and blogcfc
		Object[] params;
		try {
			Integer.parseInt(id);
			
			params = new Object[]{
				Integer.parseInt(id),
				store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
				store.getString(PreferenceConstants.P_BLOGGING_PASSWORD)
			};
		} catch (java.lang.NumberFormatException jlnfe) {
			params = new Object[]{
				id,
				store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
				store.getString(PreferenceConstants.P_BLOGGING_PASSWORD)
			};
		}
		
		HashMap posting = (HashMap)client.execute("metaWeblog.getPost", params);
		
		//this should make the posting header information into proper 
		//afae headers
		translateHeadersIncoming(posting);
		
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject("Blog - " + store.getString(PreferenceConstants.P_BLOGGING_ID));
		
		if (!project.exists()) project.create(null);
		if (!project.isOpen()) project.open(null);
		
		IContainer container = project;
		
		String title = (String)posting.get("Title");
		String filetitle = title.replace('/','_');
		filetitle = filetitle.replace('\\','_');
		
		final IFile file = container.getFile(new Path(filetitle + ".blog"));
		
		Set keys = posting.keySet();
		Iterator i = keys.iterator();
		
		String contents = "";
		
		while(i.hasNext()) {
			String key = (String)i.next();
			
			if(!key.equals(ITEM_DESCRIPTION)){
				contents += key + ": " + posting.get(key) + "\n";
			}
		}
		
		//end header section
		contents += "\n";
		
		contents += posting.get(ITEM_DESCRIPTION);
		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		
		if (file.exists()) {
			file.setContents(stream, true, true, null);
		} else {
			file.create(stream, true, null);
		}
		stream.close();
		
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IDE.openEditor(page, file, true);
	}
	
	public Object[] getRecentPosts() throws XmlRpcException, MalformedURLException {
		Object[] postings = null;
				
		XmlRpcClient client = createClient();
		
		Object[] params = new Object[]{
			store.getString(PreferenceConstants.P_BLOGGING_ID),
			store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
			store.getString(PreferenceConstants.P_BLOGGING_PASSWORD),
			new Integer(20)
		};
	
		//Object result;
		postings = (Object[])client.execute("metaWeblog.getRecentPosts", params);
		
		int plen = postings.length;
		for(int q=0; q<plen; q++){
			translateHeadersIncoming((HashMap)postings[q]);
		}
		
		return postings;
	}

	@SuppressWarnings("unchecked")
	public String postEntry() {			
		String postid = null;
		boolean update_complete = false;
		
		/////////////////////
		IEditorPart part = getEditorPart();
		ITextEditor editor = (ITextEditor)part;
		IEditorInput input = part.getEditorInput();
		/////////////////////
		
		/////////////////////
		ProgressMonitorDialog pmd = getProgressDialog();
		IProgressMonitor pm = pmd.getProgressMonitor();
		/////////////////////
		
		pmd.open();
		pm.beginTask("Posting Entry", 3);
		
		editor.doSave(null);
		pm.worked(1);
		
		IDocument doc = editor.getDocumentProvider().getDocument(input);
		
		Object[] header_contents = null;
		try {
			pm.subTask("Parsing Headers");
			header_contents = parseHeaderInformation(doc);
			
			XmlRpcClient client = createClient();
			
			HashMap struct = new HashMap();
			struct.putAll((HashMap)header_contents[0]);
			struct.put(
				translations.get(ITEM_DESCRIPTION), 
				header_contents[1]
			);
			
			pm.worked(1);
			
			//do a check for a title header, if it's not there it's probably not a blog
			//post and they might have accedently pushed the post button. Ask them
			//if they want to post it, and dump out if they dont.
			if(!struct.containsKey(translations.get(ITEM_TITLE))) {
				boolean to_continue = false;
				
				final Shell parent = part.getSite().getShell();
				while(true) {
					MessageBox box = new MessageBox(
						parent,
						SWT.YES | SWT.NO | SWT.APPLICATION_MODAL | SWT.ICON_WARNING
					);
					box.setText("Missing Title Header");
					box.setMessage("This file doesn't seem to be a blog post. Should I post it anyway?");
					
					int ret = box.open();
					
					if(ret == SWT.YES) {
						to_continue = true;
						break;
					} else if(ret == SWT.NO) {
						to_continue = false;
						break;
					}
				}
				//they didn't mean to post
				if(!to_continue) {
					pm.setCanceled(true);
					pm.done();
					pmd.close();
					throw new IllegalArgumentException();
				}
			}
			
			//one last chance to cancel before we send
			if(pm.isCanceled()) {
				pm.setCanceled(true);
				pm.done();
				pmd.close();
			}
			
			pm.subTask("Posting to Blog");
			//if the postid exists this is an edit, else it's and add
			if(struct.containsKey(translations.get(ITEM_ID))) {
				/*
				 * String postid, 
					String username, 
					String password, 
					struct content, 
					boolean publish
				 */
				String trankey = translations.get(ITEM_ID);
				String pid = struct.get(trankey).toString();
				
				Object[] params = new Object[]{
					pid,
					store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
					store.getString(PreferenceConstants.P_BLOGGING_PASSWORD),
					struct,
					Boolean.TRUE
				};
				
				Boolean worked = (Boolean)client.execute("metaWeblog.editPost", params);
				
				if(worked) {
					update_complete = true;
					postid = pid;
				}
			} else {
				/*
				 * String blogid, 
				 * String username, 
				 * String password, 
				 * struct content, 
				 * boolean publish
				 */
				Object[] params = new Object[]{
					store.getString(PreferenceConstants.P_BLOGGING_ID),
					store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
					store.getString(PreferenceConstants.P_BLOGGING_PASSWORD),
					struct,
					Boolean.TRUE
				};
				
				postid = (String)client.execute("metaWeblog.newPost", params);
				if(postid != null && !"".equals(postid))
					update_complete = true;
			}
			
			pm.worked(1);
			
			//if it looks like we got a good post, reload the entry. This should
			//add some headers and give indication that the posting worked.
			if(update_complete){
				try {
					this.getPost(postid);
					notifyPost();
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (XmlRpcException e) {
			e.printStackTrace();
		} finally {
			pm.done();
			pmd.close();
		}
		
		return postid;
	}
		
	/*
	 * metaWeblog.newMediaObject 
	 *
	 * metaWeblog.newMediaObject (blogid, username, password, struct) returns struct
	 * The blogid, username and password params are as in the Blogger API. 
	 * 
	 * The struct must contain at least three elements, name, type and bits.
	 * 
	 * name is a string, it may be used to determine the name of the file that stores 
	 * the object, or to display it in a list of objects. It determines how the weblog 
	 * refers to the object. If the name is the same as an existing object stored in the 
	 * weblog, it may replace the existing object.
	 * 
	 * type is a string, it indicates the type of the object, it's a standard MIME type, 
	 * like audio/mpeg or image/jpeg or video/quicktime. 
	 * 
	 * bits is a base64-encoded binary value containing the content of the object.
	 * 
	 * The struct may contain other elements, which may or may not be stored by the content management system.
	 * 
	 * If newMediaObject fails, it throws an error. If it succeeds, it returns a struct, which must 
	 * contain at least one element, url, which is the url through which the object can be accessed. It must 
	 * be either an FTP or HTTP url.
	 */
	@SuppressWarnings("unchecked")
	public String postMedia(String name, String type, Base64Data bits) {
		/////////////////////
		ProgressMonitorDialog pmd = getProgressDialog();
		IProgressMonitor pm = pmd.getProgressMonitor();
		/////////////////////
		pmd.open();
		pm.beginTask("Uploading Media", 3);
		
		XmlRpcClient client;
		try {
			client = createClient();
			
			pm.subTask("Preparing to Send");
			
			HashMap struct = new HashMap();
			struct.put("name", name);
			struct.put("type", type);
			struct.put("bits", bits);
			
			pm.worked(1);
			
			pm.subTask("Uploading " + name + "(" + type + ")");
			
			Object[] params = new Object[]{
				store.getString(PreferenceConstants.P_BLOGGING_ID),
				store.getString(PreferenceConstants.P_BLOGGING_USERNAME), 
				store.getString(PreferenceConstants.P_BLOGGING_PASSWORD),
				struct
			};
			HashMap returns;
			
			returns = (HashMap)client.execute("metaWeblog.newMediaObject", params);
			
			pm.worked(1);
			
			return (String)returns.get("url");
		} catch (XmlRpcException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} finally {
			pm.done();
			pmd.close();
		}
			
		return null;
	}
	
	private ProgressMonitorDialog getProgressDialog() {
		IWorkbench workbench = Activator.getDefault().getWorkbench();
		IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(page.getActivePart().getSite().getShell());
		return pmd;
	}
	
	private IEditorPart getEditorPart() {
		IWorkbench workbench = Activator.getDefault().getWorkbench();
		IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();

		IEditorPart part = page.getActiveEditor();
		return part;
	}
	
	
	
	/**
	 * Parses a document and gets the blog posting headers and body into the Object.
	 * the object will contain a hash map of header data [0] and the entire body
	 * information [1]
	 * @param doc
	 * @return Object[] with 0 being a hashmap of headers, and 1 being the posting body
	 * @throws BadLocationException
	 */
	private Object[] parseHeaderInformation(IDocument doc) throws BadLocationException {
		Object[] header_contents = new Object[2];
		String strWholeFile[] = doc.get().split( doc.getLineDelimiter(0) );
		HashMap<String,Object> headers = new HashMap<String,Object>();
		StringBuffer contents = new StringBuffer();
		int intLines = strWholeFile.length;
		boolean boolHeaderDone = false;
		
		for(int q=0; q<intLines; q++) {
			if(!boolHeaderDone) {
				if( "".equals(strWholeFile[q]) ) {
					boolHeaderDone = true;
				} else {
					String[] name_value = strWholeFile[q].split(":");
					if(name_value.length == 2)
						translateHeadersToSend(name_value[0], name_value[1], headers);
				}
			} else {
				contents.append(strWholeFile[q] + "\n");
			}
		}
		
		header_contents[0] = headers;
		header_contents[1] = contents.toString();
		
		return header_contents;
	}
	
	/**
	 * This parses the header information from a blog get and changes the map object
	 * to have headers that are a bit more friendly (if possible) see the translation
	 * maps in this object. Basically, this is so stuff like "wp_title" looks like "Title:"
	 * to the client posting the entry
	 * @param map
	 */
	@SuppressWarnings("unchecked")
	private void translateHeadersIncoming(HashMap map) {
		//Set keys = map.keySet();
		Object[] keys = map.keySet().toArray();
		//Iterator i = keys.iterator();
		
		//while(i.hasNext()) {
		for(int q=0; q<keys.length; q++){
			//String key = (String)i.next();
			String key = keys[q].toString();
			
			//System.err.println(key);
			
			if(translations_reverse.containsKey(key)) {
				//if this is categories, make a comma list out of it
				if(ITEM_CATEGORIES.equals( (String)translations_reverse.get(key) ) ) {
					String cat_commalist = "";
					
					//get the categories, they could be an object list or a string so
					//we have to be careful
					Object cat_guess = map.get(key);
					
					//it's a string so just use the value
					if(cat_guess instanceof String) {
						cat_commalist = cat_guess.toString();
					} else {
						try {
							//Assume it's an object list and try to make
							//a commma list out of it
							Object[] cats = (Object[])map.get(key);
							
							int catlen = cats.length;
							
							for(int z=0; z<catlen; z++) {
								cat_commalist += cats[z].toString().trim() + ", ";
							}
							
							if(cat_commalist.length() > 3)
								cat_commalist = cat_commalist.substring(0, cat_commalist.length()-2);
						} catch(Exception e) {
							//I give up, who knows what the categories are
							e.printStackTrace();
						}
					}
					
					map.remove(key);
					map.put(ITEM_CATEGORIES, cat_commalist);
				//if we get allow comments or allow pings translate those to booleans
				/* } else if(ITEM_ALLOW_COMMENTS.equals( (String)translations_reverse.get(key) ) ){
					Object o = map.get(key);
					String v = "false";
					if(!"0".equals(o.toString()))
						v = "true";
					
					map.put(ITEM_ALLOW_COMMENTS, v);
					
				} else if(ITEM_ALLOW_PINGS.equals( (String)translations_reverse.get(key) )) {
					Object o = map.get(key);
					String v = "false";
					if(!"0".equals(o.toString()))
						v = "true";
					
					map.put(ITEM_ALLOW_PINGS, v);
				*/	
				} else {
					Object o = map.get(key);
					if(o instanceof String) {
						o = o.toString().trim();
					}
					map.remove(key);
					map.put((String)translations_reverse.get(key), o);
				}
			}
			
		}
	}
	
	/**
	 * This is the reverse of translateHeadersIncoming. It takes things like "Title:" and
	 * translates them into things like "wp_title"
	 * @param name
	 * @param value
	 * @param map
	 */
	private void translateHeadersToSend(String name, String value, HashMap<String,Object> map) {
		if(translations.containsKey(name)) {
			if(ITEM_CATEGORIES.equals(name)) {
				String[] stritems = value.trim().split(",");
				Object[] items = new Object[stritems.length];
				
				for(int z=0; z<stritems.length; z++) {
					items[z] = (Object)stritems[z].trim();
				}
				
				map.put(translations.get(name), items);
			} else {
				String v = value.trim();
				//check to see if it's a boolean value
				/* if(v.toLowerCase() == "true" || v.toLowerCase() == "false") {
					//Boolean val = Boolean.FALSE;
					Integer val = new Integer(0);
					if("true".equals(v.toLowerCase()))
						//val = Boolean.TRUE;
						val = new Integer(2);
					
					map.put(translations.get(name), val);
				} else { */
					//ok not a boolean, try to parse it as an integer and if that
					//fails just do a string value
					try {
						int t;
						if("0".equals(v)) {
							map.put(translations.get(name), new Integer(0));
						} else {
							t = Integer.parseInt(v);
							map.put(translations.get(name), new Integer(t));
						}
						
					} catch (Exception e){
						map.put(translations.get(name), v);
					}
				//}	
			}
		} else {
			map.put(name, value);
		}
	}
}
