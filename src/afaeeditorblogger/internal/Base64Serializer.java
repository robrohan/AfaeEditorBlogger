package afaeeditorblogger.internal;

import org.apache.xmlrpc.serializer.StringSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A Serializer for xmlrpc to take Base64Data and wrap it
 * with it's own tag
 * @author robrohan
 *
 */
public class Base64Serializer extends StringSerializer {
	
	public void write(ContentHandler pHandler, Object pObject) throws SAXException {
		write(pHandler, "base64", pObject.toString());
	}
	
}