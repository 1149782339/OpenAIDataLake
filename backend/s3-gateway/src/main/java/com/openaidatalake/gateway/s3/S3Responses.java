package com.openaidatalake.gateway.s3;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class S3Responses {

    private S3Responses() {
    }

    public static void error(HttpServletResponse response, int status, String code, String message, String resource)
            throws IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Error>
                  <Code>%s</Code>
                  <Message>%s</Message>
                  <Resource>%s</Resource>
                  <RequestId>%s</RequestId>
                </Error>
                """.formatted(xml(code), xml(message), xml(resource), requestId);
        response.setHeader("x-amz-request-id", requestId);
        write(response, status, "application/xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    public static void listBuckets(HttpServletResponse response, List<String> buckets) throws IOException {
        String created = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Owner><ID>lake</ID><DisplayName>lake</DisplayName></Owner>
                  <Buckets>
                """);
        for (String bucket : buckets) {
            xml.append("    <Bucket><Name>").append(xml(bucket)).append("</Name><CreationDate>")
                    .append(created).append("</CreationDate></Bucket>\n");
        }
        xml.append("""
                  </Buckets>
                </ListAllMyBucketsResult>
                """);
        write(response, 200, "application/xml", xml.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void location(HttpServletResponse response) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/"></LocationConstraint>
                """;
        write(response, 200, "application/xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    public static void headBucket(HttpServletResponse response) {
        response.setStatus(200);
        response.setHeader("x-amz-bucket-region", "us-east-1");
    }

    public static void listObjects(HttpServletResponse response,
                                    String bucket,
                                    String prefix,
                                    String delimiter,
                                    List<String> commonPrefixes,
                                    int maxKeys,
                                    boolean truncated,
                                    String nextToken,
                                    boolean urlEncoding) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                """);
        xml.append("<Name>").append(xml(bucket)).append("</Name>");
        xml.append("<Prefix>").append(xml(encodeKey(prefix, urlEncoding))).append("</Prefix>");
        xml.append("<KeyCount>").append(commonPrefixes.size()).append("</KeyCount>");
        xml.append("<MaxKeys>").append(maxKeys).append("</MaxKeys>");
        if (delimiter != null && !delimiter.isEmpty()) {
            xml.append("<Delimiter>").append(xml(delimiter)).append("</Delimiter>");
        }
        if (urlEncoding) {
            xml.append("<EncodingType>url</EncodingType>");
        }
        xml.append("<IsTruncated>").append(truncated).append("</IsTruncated>");
        if (truncated && nextToken != null) {
            xml.append("<NextContinuationToken>").append(xml(encodeKey(nextToken, urlEncoding))).append("</NextContinuationToken>");
        }
        for (String common : commonPrefixes) {
            xml.append("<CommonPrefixes><Prefix>").append(xml(encodeKey(common, urlEncoding))).append("</Prefix></CommonPrefixes>");
        }
        xml.append("</ListBucketResult>");
        write(response, 200, "application/xml", xml.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String encodeKey(String value, boolean urlEncoding) {
        if (!urlEncoding || value == null) {
            return value == null ? "" : value;
        }
        return AwsUri.encodePath(value);
    }

    private static void write(HttpServletResponse response, int status, String contentType, byte[] body) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
}
