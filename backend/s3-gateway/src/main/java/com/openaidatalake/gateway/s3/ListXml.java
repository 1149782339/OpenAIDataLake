package com.openaidatalake.gateway.s3;

import com.openaidatalake.domain.path.PathPrefixes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ListXml {

    private ListXml() {
    }

    public static byte[] rewrite(byte[] body,
                                  String virtualBucket,
                                  String logicalPrefix,
                                  String physicalPrefix,
                                  String logicalListPrefix,
                                  boolean clientWantsUrlEncoding) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
            Element root = document.getDocumentElement();
            int kept = 0;
            List<Element> removed = new ArrayList<>();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String name = element.getLocalName();
                if ("Name".equals(name)) {
                    element.setTextContent(virtualBucket);
                } else if ("Prefix".equals(name)) {
                    element.setTextContent(present(logicalListPrefix, clientWantsUrlEncoding));
                } else if ("EncodingType".equals(name) && !clientWantsUrlEncoding) {
                    removed.add(element);
                } else if ("Contents".equals(name)) {
                    if (rewriteKey(element, "Key", logicalPrefix, physicalPrefix, clientWantsUrlEncoding)) {
                        kept++;
                    } else {
                        removed.add(element);
                    }
                } else if ("CommonPrefixes".equals(name)) {
                    if (!rewriteKey(element, "Prefix", logicalPrefix, physicalPrefix, clientWantsUrlEncoding)) {
                        removed.add(element);
                    } else {
                        kept++;
                    }
                }
            }
            for (Element element : removed) {
                root.removeChild(element);
            }
            for (Element element : childrenOf(root, "KeyCount")) {
                element.setTextContent(Integer.toString(kept));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("列举结果无法改写", ex);
        }
    }

    private static boolean rewriteKey(Element parent,
                                       String childName,
                                       String logicalPrefix,
                                       String physicalPrefix,
                                       boolean clientWantsUrlEncoding) {
        List<Element> keys = childrenOf(parent, childName);
        if (keys.isEmpty()) {
            return false;
        }
        Element key = keys.get(0);
        String decoded = AwsUri.decodePercent(key.getTextContent());
        String logical = PathPrefixes.toLogical(physicalPrefix, logicalPrefix, decoded);
        if (logical == null) {
            return false;
        }
        key.setTextContent(present(logical, clientWantsUrlEncoding));
        return true;
    }

    private static String present(String value, boolean urlEncoding) {
        if (!urlEncoding || value == null) {
            return value == null ? "" : value;
        }
        return AwsUri.encodePath(value);
    }

    private static List<Element> childrenOf(Element parent, String localName) {
        List<Element> found = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                found.add(element);
            }
        }
        return found;
    }
}
