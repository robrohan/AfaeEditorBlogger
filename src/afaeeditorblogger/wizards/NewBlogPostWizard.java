package afaeeditorblogger.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.preference.IPreferenceStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import afaeeditorblogger.Activator;
import afaeeditorblogger.internal.IWeblog;
import afaeeditorblogger.preferences.PreferenceConstants;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "blog". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class NewBlogPostWizard extends Wizard implements INewWizard {
	private NewBlogPostWizardPage page;
	private ISelection selection;
	private String title = "";

	/**
	 * Constructor for NewBlogPostWizard.
	 */
	public NewBlogPostWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewBlogPostWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		//final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					//save the posts in the .metadata dir (by default)
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					doFinish(store.getString(PreferenceConstants.P_BLOGGING_WORKING_DIR), fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(String containerName, String title, IProgressMonitor monitor) throws CoreException {
		this.title = title;
		
		/* This is totally stupid. Because eclipse is retarded when it comes to opening 
		 * external files, we have to make a project for posting. Absolutly retarded.
		 * This is probably the stupidest thing I've ever heard of. And to punish everyone
		 * they made JavaFileEditorInput unavailable simply, it seems, to piss people off
		 * since that was the only way to open a file out side of the workspace.
		 * 
		 * What the hell?
		 */
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject("Blog - " + store.getString(PreferenceConstants.P_BLOGGING_ID));
		
		if (!project.exists()) project.create(null);
		if (!project.isOpen()) project.open(null);
		
		IContainer container = project;
		
		//try to remove bad file characters
		String filetitle = title.replace('/','_');
		filetitle = filetitle.replace('\\','_');
		
		final IFile file = container.getFile(new Path(filetitle + ".blog"));
		monitor.beginTask("Creating " + title, 2 );
		
		try {
			InputStream stream = openContentStream();
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}
	
	/**
	 * We will initialize file contents with a sample text.
	 * I am adding this so the file get's recompiled
	 */
	private InputStream openContentStream() {
		String contents = "";
		contents += IWeblog.ITEM_TITLE + ": " + this.title + "\n";
		contents += IWeblog.ITEM_ALLOW_COMMENTS + ": 1\n";
		contents += IWeblog.ITEM_ALLOW_PINGS + ": 1\n";
		contents += IWeblog.ITEM_CATEGORIES + ": Uncategorized\n";
		
		contents += "\n";
		contents += "<p></p>";
		
		return new ByteArrayInputStream(contents.getBytes());
	}

	/*private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "AfaeEditorBlogger", IStatus.OK, message, null);
		throw new CoreException(status);
	}*/

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}