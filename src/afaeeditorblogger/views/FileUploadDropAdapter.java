package afaeeditorblogger.views;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;

import afaeeditorblogger.internal.Base64;
import afaeeditorblogger.internal.Base64Data;
import afaeeditorblogger.internal.IWeblog;
import afaeeditorblogger.internal.MetaWeblog;

public class FileUploadDropAdapter extends ViewerDropAdapter {
	private static Map<String, String> supported_types;

	private Clipboard clipboard;

	public FileUploadDropAdapter(TableViewer viewer, Clipboard clipboard) {
		super(viewer);
		this.clipboard = clipboard;

		if (supported_types == null) {
			supported_types = new HashMap<String, String>();
			// ///// IMAGES
			supported_types.put("gif", "image/gif");
			supported_types.put("jpe", "image/jpeg");
			supported_types.put("jpeg", "image/jpeg");
			supported_types.put("jpg", "image/jpeg");
			supported_types.put("png", "image/png");
			supported_types.put("bmp", "image/bmp");
			supported_types.put("tif", "image/tiff");
			supported_types.put("tiff", "image/tiff");
			// ///// FLASH
			supported_types.put("swf", "application/x-shockwave-flash");
			// ///// MPEG
			supported_types.put("mpe", "video/mpeg");
			supported_types.put("mpeg", "video/mpeg");
			supported_types.put("mpg", "video/mpeg");
			// ///// QUICKTIME
			supported_types.put("mov", "video/quicktime");
			supported_types.put("qt", "video/quicktime");
			// ///// MP3
			supported_types.put("mp2", "audio/mpeg");
			supported_types.put("mpga", "audio/mpeg");
			supported_types.put("mp3 ", "audio/mpeg");
			// ///// TEXT
			supported_types.put("txt", "text/plain");
			supported_types.put("xml", "text/xml");
			supported_types.put("htm", "text/html");
			supported_types.put("html", "text/html");
			supported_types.put("xhtml", "text/html");
			// ///// PDF
			supported_types.put("pdf", "application/pdf");
			// ///// ZIP FILES
			supported_types.put("bz2", "application/x-bzip2");
			supported_types.put("gz", "application/x-gzip");
			supported_types.put("zip", "application/zip");
		}
	}

	/**
	 * Method declared on ViewerDropAdapter
	 */
	public boolean performDrop(Object data) {

		if (data instanceof String[]) {
			String[] files = (String[]) data;
			IWeblog weblog = new MetaWeblog();
			String clipboard_contents = "";

			int flen = files.length;
			for (int x = 0; x < flen; x++) {
				try {
					// see if it's a supported mime type, and cancel the
					// drop if it is not.
					String mime = guessMimeType(files[x]);
					if (mime == null)
						return false;

					File f = new File(files[x]);
					FileInputStream fis = new FileInputStream(f);
					BufferedInputStream bis = new BufferedInputStream(fis);
					// InputStream b64is = new Base64.InputStream( bis,
					// Base64.ENCODE );
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					int b;
					while ((b = bis.read()) != -1) {
						baos.write(b);
					}
					
					Base64Data s = new Base64Data();
					s.setStorage(Base64.encodeBytes(baos.toByteArray()));

					String url = weblog.postMedia(guessFileName(files[x]),
							guessMimeType(files[x]), s);

					clipboard_contents += url;

					if (flen > 1)
						clipboard_contents += "\n";
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// how to set the clipboard
			if (!"".equals(clipboard_contents)) {
				clipboard.clearContents();
				TextTransfer textTransfer = TextTransfer.getInstance();
				clipboard.setContents(new Object[] { clipboard_contents },
						new Transfer[] { textTransfer });
			}

			return true;
		}
		return false;
	}

	private String guessFileName(String fname) {
		// Fix for windows - file.separator is \ and that breaks the regex
		String fsep = System.getProperty("file.separator");
		if ("\\".equals(fsep)) {
			fsep = "\\\\";
		}
		String[] parts = fname.split(fsep);
		return parts[parts.length - 1];
	}

	private String guessMimeType(String fname) {
		String[] parts = fname.split("\\.");
		String mime = supported_types.get(parts[(parts.length - 1)]
				.toLowerCase());
		return mime;
	}

	/**
	 * Method declared on ViewerDropAdapter
	 */
	public boolean validateDrop(Object target, int op, TransferData type) {
		// System.err.println("Target: " + target + " Op: " + op + "
		// TransferData: " + type);
		return FileTransfer.getInstance().isSupportedType(type);
	}
}