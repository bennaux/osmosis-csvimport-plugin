package net.bennokue.java.osmosis;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads a XML file and lets you evaluate a XPath String against it.
 *
 * @author bennokue
 */
public class XMLFlattener {

    /**
     * The loaded XML file as DOM Document.
     */
    private final Document xmlDocument;

    /**
     * Load the XML file and make a DOM Document out of it. Afterwards, XPath
     * Expressions can be evaluated with
     * {@link #getXPathAsArray(java.lang.String) getXPathAsArray(...)}.
     *
     * @param inputFile The input XML file.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public XMLFlattener(File inputFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(false);
        DocumentBuilder builder = docFactory.newDocumentBuilder();
        this.xmlDocument = builder.parse(inputFile);
    }

    /**
     * Evaluate a XPath Expression against the loaded XML file and return the
     * results as String array.
     *
     * @param xpathString The XPath Expression as String.
     * @return The result as String[].
     * @throws XPathExpressionException
     */
    public String[] getXPathAsArray(String xpathString) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expression = xpath.compile(xpathString);

        NodeList nodeList = (NodeList) expression.evaluate(this.xmlDocument, XPathConstants.NODESET);

        String[] result = new String[nodeList.getLength()];
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            result[i] = node.getNodeValue();
        }

        return result;
    }
}
