package com.openaidatalake.gateway.s3;

import com.openaidatalake.domain.config.LakeProperties;
import com.openaidatalake.domain.path.PathPrefixes;
import com.openaidatalake.gateway.access.GatewayAccess;
import com.openaidatalake.gateway.access.GatewayAccess.Action;
import com.openaidatalake.gateway.access.GatewayAccess.Caller;
import com.openaidatalake.gateway.access.GatewayAccess.Target;
import com.openaidatalake.gateway.forward.BackendForwarder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class S3GatewayFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(S3GatewayFilter.class);
    private static final Set<String> AUTH_QUERY = Set.of(
            "X-Amz-Algorithm", "X-Amz-Credential", "X-Amz-Date", "X-Amz-Expires",
            "X-Amz-SignedHeaders", "X-Amz-Signature", "X-Amz-Security-Token", "X-Amz-Content-Sha256");

    private final GatewayAccess access;
    private final BackendForwarder forwarder;
    private final LakeProperties properties;

    public S3GatewayFilter(GatewayAccess access, BackendForwarder forwarder, LakeProperties properties) {
        this.access = access;
        this.forwarder = forwarder;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, java.io.IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            dispatch(request, response);
        } catch (S3Exception ex) {
            if (!response.isCommitted()) {
                S3Responses.error(response, ex.status(), ex.code(), ex.getMessage(), uri);
            }
        } catch (Exception ex) {
            log.error("S3 请求失败 {}", uri, ex);
            if (!response.isCommitted()) {
                S3Responses.error(response, 500, "InternalError", "Internal error", uri);
            }
        }
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        if (query.has("X-Amz-Security-Token") || request.getHeader("x-amz-security-token") != null) {
            throw new S3Exception(501, "NotImplemented", "Temporary credentials are not supported");
        }
        String accessKey = S3Auth.accessKey(request, query);
        GatewayAccess.Auth auth = access.authenticate(accessKey);
        if (auth.unknown()) {
            throw new S3Exception(403, "InvalidAccessKeyId", "The AWS Access Key Id you provided does not exist");
        }
        if (auth.disabled()) {
            throw new S3Exception(403, "AccessDenied", "Access Denied");
        }
        S3Auth.verify(request, query, auth.caller().secret());
        String payloadHash = S3Auth.payloadHash(request, query);
        if (payloadHash.startsWith("STREAMING-")) {
            throw new S3Exception(501, "NotImplemented", "aws-chunked streaming payload is not supported");
        }
        Location location = locate(request);
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (location.bucket() == null) {
            if ("GET".equals(method)) {
                S3Responses.listBuckets(response, access.buckets(auth.caller().userId()));
                return;
            }
            throw new S3Exception(501, "NotImplemented", "Only ListBuckets is supported on /");
        }
        try {
            PathPrefixes.checkBucket(location.bucket());
        } catch (IllegalArgumentException ex) {
            throw new S3Exception(400, "InvalidBucketName", ex.getMessage());
        }
        String key;
        try {
            key = PathPrefixes.normalizeKey(location.key());
        } catch (IllegalArgumentException ex) {
            throw new S3Exception(400, "InvalidArgument", ex.getMessage());
        }
        if (query.has("uploadId") || query.has("uploads") || query.has("delete")) {
            throw new S3Exception(501, "NotImplemented", "This operation is not supported");
        }
        if (key.isEmpty() && "GET".equals(method) && query.has("location")) {
            bucketProbe(response, auth.caller(), location.bucket(), true);
            return;
        }
        if (key.isEmpty() && "HEAD".equals(method)) {
            bucketProbe(response, auth.caller(), location.bucket(), false);
            return;
        }
        if (key.isEmpty() && "GET".equals(method)) {
            list(response, auth.caller(), location.bucket(), query);
            return;
        }
        if (!key.isEmpty() && "GET".equals(method)) {
            object(request, response, auth.caller(), location.bucket(), key, Action.READ, "GET", query, null);
            return;
        }
        if (!key.isEmpty() && "HEAD".equals(method)) {
            object(request, response, auth.caller(), location.bucket(), key, Action.READ, "HEAD", query, null);
            return;
        }
        if (!key.isEmpty() && "DELETE".equals(method)) {
            object(request, response, auth.caller(), location.bucket(), key, Action.DELETE, "DELETE", query, null);
            return;
        }
        if (!key.isEmpty() && "PUT".equals(method)) {
            if (request.getHeader("x-amz-copy-source") != null) {
                throw new S3Exception(501, "NotImplemented", "CopyObject is not supported");
            }
            put(request, response, auth.caller(), location.bucket(), key, payloadHash);
            return;
        }
        throw new S3Exception(501, "NotImplemented", "This operation is not supported");
    }

    private void bucketProbe(HttpServletResponse response, Caller caller, String bucket, boolean location) throws Exception {
        switch (access.bucket(caller.userId(), bucket)) {
            case NO_BUCKET -> S3Responses.error(response, 404, "NoSuchBucket", "The specified bucket does not exist", "/" + bucket);
            case DENIED -> S3Responses.error(response, 403, "AccessDenied", "Access Denied", "/" + bucket);
            case OK -> {
                if (location) {
                    S3Responses.location(response);
                } else {
                    S3Responses.headBucket(response);
                }
            }
        }
    }

    private void list(HttpServletResponse response, Caller caller, String bucket, QueryParameters query) throws Exception {
        String prefix = query.first("prefix") == null ? "" : query.first("prefix");
        try {
            prefix = PathPrefixes.normalizeKey(prefix);
        } catch (IllegalArgumentException ex) {
            throw new S3Exception(400, "InvalidArgument", ex.getMessage());
        }
        GatewayAccess.ListAccess result = access.list(caller.userId(), bucket, prefix, query.first("delimiter"));
        switch (result.kind()) {
            case NO_BUCKET -> S3Responses.error(response, 404, "NoSuchBucket", "The specified bucket does not exist", "/" + bucket);
            case DENIED -> S3Responses.error(response, 403, "AccessDenied", "Access Denied", "/" + bucket + "/" + prefix);
            case OK -> {
                if (result.forward() != null) {
                    forwardList(response, result.forward(), bucket, prefix, query);
                } else {
                    virtualList(response, bucket, prefix, query, result.commonPrefixes());
                }
            }
        }
    }

    private void forwardList(HttpServletResponse response, Target target, String bucket, String prefix, QueryParameters query) throws Exception {
        List<QueryParameters.Param> params = new ArrayList<>();
        params.add(new QueryParameters.Param("list-type", "2"));
        params.add(new QueryParameters.Param("encoding-type", "url"));
        if (!target.physicalKey().isEmpty()) {
            params.add(new QueryParameters.Param("prefix", target.physicalKey()));
        }
        copyQuery(query, params, "delimiter");
        copyQuery(query, params, "max-keys");
        copyQuery(query, params, "continuation-token");
        copyQuery(query, params, "start-after");
        String canonical = QueryParameters.of(params).canonical(Set.of());
        var uri = BackendForwarder.endpoint(target.endpoint(), target.pathStyle(), target.physicalBucket(), "", canonical);
        forwarder.forward(new BackendForwarder.Call(
                "GET", uri, target.region(), target.accessKey(), target.secret(),
                Map.of(), SigV4.EMPTY_SHA256, null,
                new BackendForwarder.ListRewrite(bucket, target.logicalPrefix(), target.physicalPrefix(), prefix, urlEncoding(query))),
                response);
    }

    private void virtualList(HttpServletResponse response, String bucket, String prefix, QueryParameters query, List<String> all) throws Exception {
        int maxKeys = maxKeys(query);
        String token = query.first("continuation-token");
        if (token == null) {
            token = query.first("start-after");
        }
        List<String> page = new ArrayList<>();
        boolean truncated = false;
        for (String common : all) {
            if (token != null && common.compareTo(token) <= 0) {
                continue;
            }
            if (page.size() == maxKeys) {
                truncated = true;
                break;
            }
            page.add(common);
        }
        String delimiter = query.first("delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            delimiter = "/";
        }
        S3Responses.listObjects(response, bucket, prefix, delimiter, page, maxKeys, truncated,
                truncated ? page.get(page.size() - 1) : null, urlEncoding(query));
    }

    private void object(HttpServletRequest request, HttpServletResponse response, Caller caller,
                        String bucket, String key, Action action, String method, QueryParameters query, Path body) throws Exception {
        GatewayAccess.ObjectAccess result = access.object(caller.userId(), bucket, key, action);
        switch (result.kind()) {
            case NO_BUCKET -> S3Responses.error(response, 404, "NoSuchBucket", "The specified bucket does not exist", "/" + bucket);
            case DENIED -> S3Responses.error(response, 403, "AccessDenied", "Access Denied", resource(bucket, key));
            case NO_SUCH_KEY -> S3Responses.error(response, 404, "NoSuchKey", "The specified key does not exist", resource(bucket, key));
            case OK -> {
                Target target = result.target();
                String canonical = query.without(AUTH_QUERY).canonical(Set.of());
                var uri = BackendForwarder.endpoint(target.endpoint(), target.pathStyle(), target.physicalBucket(), target.physicalKey(), canonical);
                String hash = body == null ? SigV4.EMPTY_SHA256 : SigV4.sha256Hex(Files.readAllBytes(body));
                forwarder.forward(new BackendForwarder.Call(
                        method, uri, target.region(), target.accessKey(), target.secret(),
                        headersFrom(request, body != null), hash, body, null), response);
            }
        }
    }

    private void put(HttpServletRequest request, HttpServletResponse response, Caller caller,
                     String bucket, String key, String declaredHash) throws Exception {
        long declaredLength = request.getContentLengthLong();
        if (declaredLength < 0) {
            throw new S3Exception(411, "MissingContentLength", "Content-Length is required");
        }
        if (declaredLength > properties.getMaxUploadBytes()) {
            throw new S3Exception(400, "EntityTooLarge", "Object is too large");
        }
        Path temp = Files.createTempFile("lake-put-", ".bin");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = request.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    total += read;
                    if (total > properties.getMaxUploadBytes()) {
                        throw new S3Exception(400, "EntityTooLarge", "Object is too large");
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            if (total != declaredLength) {
                throw new S3Exception(400, "InvalidRequest", "Content-Length does not match the body");
            }
            String actual = SigV4.toHex(digest.digest());
            if (isSha256(declaredHash) && !declaredHash.equalsIgnoreCase(actual)) {
                throw new S3Exception(400, "XAmzContentSHA256Mismatch", "Content hash does not match");
            }
            GatewayAccess.ObjectAccess result = access.object(caller.userId(), bucket, key, Action.WRITE);
            switch (result.kind()) {
                case NO_BUCKET -> S3Responses.error(response, 404, "NoSuchBucket", "The specified bucket does not exist", "/" + bucket);
                case DENIED -> S3Responses.error(response, 403, "AccessDenied", "Access Denied", resource(bucket, key));
                case NO_SUCH_KEY -> S3Responses.error(response, 404, "NoSuchKey", "The specified key does not exist", resource(bucket, key));
                case OK -> {
                    Target target = result.target();
                    var uri = BackendForwarder.endpoint(target.endpoint(), target.pathStyle(), target.physicalBucket(), target.physicalKey(), "");
                    forwarder.forward(new BackendForwarder.Call(
                            "PUT", uri, target.region(), target.accessKey(), target.secret(),
                            headersFrom(request, true), actual, temp, null), response);
                }
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static Map<String, String> headersFrom(HttpServletRequest request, boolean upload) {
        Map<String, String> headers = new TreeMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement().toLowerCase(Locale.ROOT);
            if (upload ? uploadHeader(name) : readHeader(name)) {
                headers.put(name, join(request.getHeaders(name)));
            }
        }
        if (upload && !headers.containsKey("content-type")) {
            headers.put("content-type", "application/octet-stream");
        }
        return headers;
    }

    private static boolean uploadHeader(String name) {
        return name.equals("content-type")
                || name.equals("content-disposition")
                || name.equals("content-encoding")
                || name.equals("content-language")
                || name.equals("content-md5")
                || name.equals("cache-control")
                || name.equals("expires")
                || name.startsWith("x-amz-meta-")
                || name.equals("x-amz-storage-class")
                || name.equals("x-amz-tagging")
                || name.equals("x-amz-acl")
                || name.startsWith("x-amz-checksum-")
                || name.equals("x-amz-sdk-checksum-algorithm")
                || name.startsWith("x-amz-server-side-encryption");
    }

    private static boolean readHeader(String name) {
        return name.equals("range")
                || name.equals("if-match")
                || name.equals("if-none-match")
                || name.equals("if-modified-since")
                || name.equals("if-unmodified-since");
    }

    private static String join(Enumeration<String> values) {
        List<String> all = new ArrayList<>();
        while (values.hasMoreElements()) {
            all.add(values.nextElement());
        }
        return String.join(",", all);
    }

    private static void copyQuery(QueryParameters query, List<QueryParameters.Param> params, String name) {
        String value = query.first(name);
        if (value != null) {
            params.add(new QueryParameters.Param(name, value));
        }
    }

    private static int maxKeys(QueryParameters query) {
        String raw = query.first("max-keys");
        int maxKeys = 1000;
        if (raw != null && !raw.isBlank()) {
            try {
                maxKeys = Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                throw new S3Exception(400, "InvalidArgument", "max-keys is not a number");
            }
        }
        if (maxKeys < 1) {
            return 1;
        }
        return Math.min(maxKeys, 1000);
    }

    private static boolean urlEncoding(QueryParameters query) {
        return "url".equals(query.first("encoding-type"));
    }

    private static boolean isSha256(String value) {
        if (value == null || value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static Location locate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty() || "/".equals(uri)) {
            return new Location(null, "");
        }
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        int slash = path.indexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            String bucket = slash < 0 ? path : path.substring(0, slash);
            if (bucket.endsWith("/")) {
                bucket = bucket.substring(0, bucket.length() - 1);
            }
            return new Location(bucket, "");
        }
        return new Location(path.substring(0, slash), path.substring(slash + 1));
    }

    private static String resource(String bucket, String key) {
        return "/" + bucket + "/" + key;
    }

    private record Location(String bucket, String key) {
    }
}
