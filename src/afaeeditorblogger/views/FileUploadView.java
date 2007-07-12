package afaeeditorblogger.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
//import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
//import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class FileUploadView extends ViewPart {
	private Clipboard clipboard;
	private TableViewer viewer;

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLabelProvider(new LabelProvider());
		
		Display display = parent.getDisplay();
		//Rectangle rect = parent.getClientArea();
		//Image newImage = new Image(display, Math.max (1, rect.width), 1);	
		Image newImage = new Image(display,16,16);	
		GC gc = new GC (newImage);
		gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		//gc.fillGradientRectangle (rect.x, rect.y, rect.width, 1, false);
		gc.fillGradientRectangle(0, 0, 16, 16, true);
		gc.dispose();
		//parent.setBackgroundImage(newImage);
		
		//final Image parliamentImage = new Image(display, "./parliament.jpg");
		Table table = viewer.getTable(); //new Table(shell, SWT.NONE);
		//table.setBounds(10,10,350,300);
		table.setBackgroundImage(newImage);
		
		clipboard = new Clipboard(getSite().getShell().getDisplay());

		// add drag and drop support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance() };
		viewer.addDropSupport(ops, transfers, new FileUploadDropAdapter(viewer,	clipboard));
	}

	public void dispose() {
		super.dispose();
		if (clipboard != null)
			clipboard.dispose();
		clipboard = null;
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
}