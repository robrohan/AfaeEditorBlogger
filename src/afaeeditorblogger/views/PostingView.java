package afaeeditorblogger.views;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import afaeeditorblogger.internal.IWeblog;
import afaeeditorblogger.internal.MetaWeblog;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class PostingView extends ViewPart implements IPropertyChangeListener {
	private TableViewer viewer;
	private Action refresh_action;
	//private Action delete_action;
	private Action doubleClickAction;

	private String[] titles = {
		"",
		IWeblog.ITEM_TITLE,
		IWeblog.ITEM_DATE,
		IWeblog.ITEM_CATEGORIES
	};
	
	public Object[] postings;
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			if(postings == null) {
				return new Object[] { 
					new Object[] {
						"Post no bills",
						"Post no bills",
						new Date(),
						"Post no bills"
					}
				}; 
			} else {
				int objsize = postings.length;
				ArrayList<Object[]> c = new ArrayList<Object[]>();
				
				for(int i=0; i<objsize; i++) {
					ArrayList<Object> row = new ArrayList<Object>();
					
					HashMap h = (HashMap)postings[i];
					
					row.add((String)h.get(IWeblog.ITEM_ID));
					row.add((String)h.get(IWeblog.ITEM_TITLE));
					row.add((Date)  h.get(IWeblog.ITEM_DATE));
					row.add((String)h.get(IWeblog.ITEM_CATEGORIES));
					
					c.add( row.toArray() );
				}
				return c.toArray();
			}
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			//return getText(obj);
			try {
				Object[] l = (Object[])obj;
				if(index == 2){
					return DateFormat.getDateInstance().format((Date)l[index]);
				} else if(index == 0){
					return "";
				} else {
					return l[index].toString();
				}
			} catch(Exception e) {
				return "";
			}
		}
		
		public Image getColumnImage(Object obj, int index) {
			if(index == 0)
				return getImage(obj);
			else
				return null;
		}
		
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FILE
			);
		}
	}
	
	class NameSorter extends ViewerSorter {
		/* (non-Javadoc)
		 * Method declared on ViewerSorter.
		 */
		public int compare(Viewer viewer, Object o1, Object o2) {
			//return o1.toString().compareTo(o2.toString());
			Date date1 = (Date)((Object[])o1)[2];
			Date date2 = (Date)((Object[])o2)[2];
			
			long time1 = date1.getTime();
			long time2 = date2.getTime();
				
			if(time1 == time2) return 0;
			if(time1 > time2)  return -1;
			if(time1 < time2)  return 1;
			
			return 0;
		}
	}

	/**
	 * The constructor.
	 */
	public PostingView() {
		
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		Table table = viewer.getTable();
					
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
				
		for (int i=0; i<titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles [i]);
		}
		table.setSortColumn(table.getColumn(2));
		table.setSortDirection(1);
		
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		refreshView();

		for(int i=0; i<titles.length; i++) {
			table.getColumn(i).pack();
		}
		
		//add this class to the property change listener list
		//on the weblog object
		new MetaWeblog().addPropertyChangeListener(this);
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PostingView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(refresh_action);
		//manager.add(new Separator());
		//manager.add(delete_action);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(refresh_action);
		//manager.add(delete_action);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(refresh_action);
		//manager.add(delete_action);
	}
	
	public void refreshView(){
		try {
			IWeblog mw = new MetaWeblog();
			postings = mw.getRecentPosts();
			
			viewer.refresh();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			showMessage("The supplied RPC URL seems to be invalid");
		} catch (XmlRpcException e) {
			e.printStackTrace();
			showMessage(e.toString());
		}
	}
	
	private void makeActions() {
		//////////////////////////////////////
		refresh_action = new Action() {
			public void run() {
				refreshView();
			}
		};
		refresh_action.setText("Refresh");
		refresh_action.setToolTipText("Refresh the entry list");
		refresh_action.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_REDO)
		);
		/////////////////////////////////////
		
		/* delete_action = new Action() {
			public void run() {
				showMessage("Not Implemented");
			}
		};
		delete_action.setText("Delete");
		delete_action.setToolTipText("Delete the entry");
		delete_action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		*/
		
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj[] = (Object[])((IStructuredSelection)selection).getFirstElement();

				try {
					
					IWeblog mw = new MetaWeblog();
					mw.getPost(obj[0].toString());
				
				} catch (MalformedURLException e) {
					e.printStackTrace();
					showMessage("The supplied RPC URL seems to be invalid");
				} catch (XmlRpcException e) {
					e.printStackTrace();
					showMessage(e.toString());
				} catch (CoreException e) {
					e.printStackTrace();
					showMessage(e.toString());
				} catch (IOException e) {
					e.printStackTrace();
					showMessage(e.toString());
				}
			
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),"Sample View",message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if( event.getProperty().equals("BlogPostingEvent")) {
			//Object val = event.getNewValue();
			this.refreshView();
		}
	}
	
	public void dispose() {
		new MetaWeblog().removePropertyChangeListener(this);
		super.dispose();
	}
	
}