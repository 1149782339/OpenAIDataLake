package com.openaidatalake.console.s3;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class S3ListXml {

    private S3ListXml() {
    }

    public record PrefixPage(List<String> prefixes, String nextToken) {
    }

    public static List<String> bucketNames(byte[] body) {
        Document document = parse(body);
        List<String> names = new ArrayList<>();
        for (Element bucket : elements(document.getDocumentElement(), "Bucket")) {
            String name = directText(bucket, "Name");
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }
        return names;
    }

    public static PrefixPage prefixes(byte[] body) {
        Document document = parse(body);
        List<String> prefixes = new ArrayList<>();
        for (Element group : elements(document.getDocumentElement(), "CommonPrefixes")) {
            String prefix = directText(group, "Prefix");
            if (prefix != null && !prefix.isBlank()) {
                prefixes.add(decode(prefix.trim()));
            }
        }
        String next = text(document.getDocumentElement(), "NextContinuationToken");
        String truncated = text(document.getDocumentElement(), "IsTruncated");
        if (next != null) {
            next = decode(next);
        }
        if (!"true".equalsIgnoreCase(truncated)) {
            next = null;
        }
        return new PrefixPage(prefixes, next);
    }

    public static String errorMessage(byte[] body) {
        try {
            Document document = parse(body);
            String message = text(document.getDocumentElement(), "Message");
            if (message != null && !message.isBlank()) {
                return message.trim();
            }
            String code = text(document.getDocumentElement(), "Code");
            return code == null || code.isBlank() ? null : code.trim();
        } catch (ProbeException ex) {
            return null;
        }
    }

    private static Document parse(byte[] body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
                // 解析器不支持时仍关闭外部实体
            }
            try {
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (Exception ignored) {
                // 部分解析器没有这两个属性
            }
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            Document document = builder.parse(new InputSource(new StringReader(new String(body, StandardCharsets.UTF_8))));
            document.getDocumentElement().normalize();
            return document;
        } catch (ProbeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProbeException("对象存储返回了无法识别的响应");
        }
    }

    private static List<Element> elements(Element root, String name) {
        List<Element> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        collect(root, name, found);
        return found;
    }

    private static void collect(Element element, String name, List<Element> found) {
        if (sameName(element, name)) {
            found.add(element);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                collect(childElement, name, found);
            }
        }
    }

    private static String directText(Element element, String name) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement && sameName(childElement, name)) {
                String text = childElement.getTextContent();
                return text == null ? null : text.trim();
            }
        }
        return null;
    }

    private static String text(Element root, String name) {
        for (Element element : elements(root, name)) {
            String value = element.getTextContent();
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean sameName(Element element, String name) {
        String local = element.getLocalName();
        return name.equals(local) || name.equals(element.getTagName());
    }

    static String decode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        byte[] bytes = new byte[value.length()];
        int length = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes[length++] = (byte) ((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            byte[] raw = String.valueOf(current).getBytes(StandardCharsets.UTF_8);
            System.arraycopy(raw, 0, bytes, length, raw.length);
            length += raw.length;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
}
