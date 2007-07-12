package afaeeditorblogger.internal;

import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.xml.sax.SAXException;

/**
 * A custom typefactory to support serializing of base64 data
 * 
 * Warning: this doesn't implement a parser for base64 data so it
 * can only send information
 * @author robrohan
 *
 */
public class BloggingTypeFactory extends TypeFactoryImpl {
	
    public BloggingTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
    	//at present we are not supporting base64 return values
        /* if (DateSerializer.DATE_TAG.equals(pLocalName)) {
            return new DateParser(pFormat);
        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        } */
    	return super.getParser(pConfig, pContext, pURI, pLocalName);
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        if (pObject instanceof Base64Data) {
        	return new Base64Serializer();
        } else {
            return super.getSerializer(pConfig, pObject);
        }
    }
}