package com.openaidatalake.gateway.forward;

import com.openaidatalake.gateway.s3.AwsUri;
import com.openaidatalake.gateway.s3.ListXml;
import com.openaidatalake.gateway.s3.QueryParameters;
import com.openaidatalake.gateway.s3.SigV4;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

@Component
public class BackendForwarder {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final Set<String> HOP = Set.of("connection", "transfer-encoding", "server", "keep-alive");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public void forward(Call call, HttpServletResponse response) throws Exception {
        String amzDate = AMZ_DATE.format(Instant.now());
        String scopeDate = amzDate.substring(0, 8);
        URI uri = call.uri();
        String host = uri.getHost();
        if (uri.getPort() != -1) {
            host = host + ":" + uri.getPort();
        }
        Map<String, String> headers = new TreeMap<>(call.headers());
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", call.payloadHash());
        String canonicalQuery = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        String canonical = SigV4.canonicalRequest(
                call.method(),
                uri.getRawPath(),
                canonicalQuery,
                headers.keySet().stream().toList(),
                headers,
                call.payloadHash());
        String signature = SigV4.signature(call.secret(), amzDate, scopeDate, call.region(), canonical);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .method(call.method(), call.body() == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofFile(call.body()));
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if ("host".equals(header.getKey())) {
                continue;
            }
            builder.header(header.getKey(), header.getValue());
        }
        builder.header("Authorization", SigV4.authorization(
                call.accessKey(), scopeDate, call.region(), String.join(";", headers.keySet()), signature));
        HttpResponse<InputStream> upstream = http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = upstream.body()) {
            if (call.listRewrite() != null && upstream.statusCode() == 200) {
                byte[] rewritten = ListXml.rewrite(
                        body.readAllBytes(),
                        call.listRewrite().virtualBucket(),
                        call.listRewrite().logicalPrefix(),
                        call.listRewrite().physicalPrefix(),
                        call.listRewrite().logicalListPrefix(),
                        call.listRewrite().urlEncoding());
                response.setStatus(200);
                response.setContentType("application/xml");
                response.setContentLength(rewritten.length);
                response.getOutputStream().write(rewritten);
                return;
            }
            response.setStatus(upstream.statusCode());
            upstream.headers().map().forEach((name, values) -> {
                if (copy(name)) {
                    for (String value : values) {
                        response.addHeader(name, value);
                    }
                }
            });
            if (!"HEAD".equals(call.method())) {
                body.transferTo(response.getOutputStream());
            }
        }
    }

    public static URI endpoint(String endpoint, boolean pathStyle, String bucket, String key, String canonicalQuery) {
        URI base = URI.create(endpoint);
        String host = pathStyle ? base.getHost() : bucket + "." + base.getHost();
        String path = pathStyle ? "/" + AwsUri.encode(bucket) : "";
        if (key != null && !key.isEmpty()) {
            path = path + "/" + AwsUri.encodePath(key);
        }
        if (path.isEmpty()) {
            path = "/";
        }
        StringBuilder url = new StringBuilder();
        url.append(base.getScheme()).append("://").append(host);
        if (base.getPort() != -1) {
            url.append(':').append(base.getPort());
        }
        url.append(path);
        if (canonicalQuery != null && !canonicalQuery.isEmpty()) {
            url.append('?').append(canonicalQuery);
        }
        return URI.create(url.toString());
    }

    public static String canonicalQuery(QueryParameters parameters) {
        return parameters.canonical(Set.of());
    }

    private static boolean copy(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (HOP.contains(lower)) {
            return false;
        }
        return lower.equals("content-type")
                || lower.equals("content-length")
                || lower.equals("etag")
                || lower.equals("last-modified")
                || lower.equals("accept-ranges")
                || lower.equals("content-range")
                || lower.equals("content-disposition")
                || lower.equals("content-encoding")
                || lower.equals("cache-control")
                || lower.equals("expires")
                || lower.startsWith("x-amz-meta-")
                || lower.startsWith("x-amz-checksum-")
                || lower.equals("x-amz-version-id")
                || lower.equals("x-amz-storage-class")
                || lower.equals("x-amz-server-side-encryption")
                || lower.equals("x-amz-delete-marker")
                || lower.equals("x-amz-expiration")
                || lower.equals("x-amz-request-charged");
    }

    public record Call(
            String method,
            URI uri,
            String region,
            String accessKey,
            String secret,
            Map<String, String> headers,
            String payloadHash,
            Path body,
            ListRewrite listRewrite
    ) {
    }

    public record ListRewrite(
            String virtualBucket,
            String logicalPrefix,
            String physicalPrefix,
            String logicalListPrefix,
            boolean urlEncoding
    ) {
    }
}
