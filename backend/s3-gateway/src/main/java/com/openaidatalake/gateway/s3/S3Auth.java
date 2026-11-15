package com.openaidatalake.gateway.s3;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class S3Auth {

    private static final Pattern AUTHORIZATION = Pattern.compile(
            "AWS4-HMAC-SHA256\\s+Credential=([^,\\s]+),\\s*SignedHeaders=([^,\\s]+),\\s*Signature=([0-9a-fA-F]{64})");
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
    private static final Duration SKEW = Duration.ofMinutes(15);
    private static final Set<String> SIGNATURE_PARAM = Set.of("X-Amz-Signature");

    private S3Auth() {
    }

    public static String accessKey(HttpServletRequest request, QueryParameters query) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            Matcher matcher = AUTHORIZATION.matcher(authorization.trim());
            if (!matcher.matches()) {
                throw new S3Exception(400, "AuthorizationHeaderMalformed", "Authorization 头无法解析");
            }
            return credential(matcher.group(1)).accessKey();
        }
        if ("AWS4-HMAC-SHA256".equals(query.first("X-Amz-Algorithm"))) {
            String credential = query.first("X-Amz-Credential");
            if (credential == null) {
                throw new S3Exception(400, "AuthorizationQueryParametersError", "缺少 X-Amz-Credential");
            }
            return credential(credential).accessKey();
        }
        throw new S3Exception(403, "AccessDenied", "Missing Authentication Token");
    }

    public static void verify(HttpServletRequest request, QueryParameters query, String secret) {
        String authorization = request.getHeader("Authorization");
        boolean headerAuth = authorization != null && !authorization.isBlank();
        String credentialText;
        String signedHeaderText;
        String presented;
        if (headerAuth) {
            Matcher matcher = AUTHORIZATION.matcher(authorization.trim());
            if (!matcher.matches()) {
                throw new S3Exception(400, "AuthorizationHeaderMalformed", "Authorization 头无法解析");
            }
            credentialText = matcher.group(1);
            signedHeaderText = matcher.group(2);
            presented = matcher.group(3);
        } else {
            credentialText = query.first("X-Amz-Credential");
            signedHeaderText = query.first("X-Amz-SignedHeaders");
            presented = query.first("X-Amz-Signature");
            if (credentialText == null || signedHeaderText == null || presented == null) {
                throw new S3Exception(400, "AuthorizationQueryParametersError", "预签名参数不完整");
            }
        }
        Credential credential = credential(credentialText);
        if (!"s3".equals(credential.service()) || !"aws4_request".equals(credential.terminator())) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "Credential 范围不正确");
        }
        String amzDate = firstNonBlank(request.getHeader("x-amz-date"), query.first("X-Amz-Date"));
        if (amzDate == null) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "缺少 x-amz-date");
        }
        Instant signedAt = parseDate(amzDate);
        if (!amzDate.startsWith(credential.date())) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "日期和 Credential 不一致");
        }
        Instant now = Instant.now();
        if (headerAuth) {
            if (signedAt.isBefore(now.minus(SKEW)) || signedAt.isAfter(now.plus(SKEW))) {
                throw new S3Exception(403, "RequestTimeTooSkewed", "请求时间超出允许范围");
            }
        } else {
            long expires = parseExpires(query.first("X-Amz-Expires"));
            if (now.isBefore(signedAt.minus(SKEW)) || now.isAfter(signedAt.plusSeconds(expires).plus(SKEW))) {
                throw new S3Exception(403, "AccessDenied", "Request has expired");
            }
        }
        List<String> signedHeaders = List.of(signedHeaderText.toLowerCase(Locale.ROOT).split(";"));
        if (!signedHeaders.contains("host")) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "签名必须包含 host");
        }
        String payloadHash = payloadHash(request, query, headerAuth);
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : signedHeaders) {
            values.put(name, headerValues(request, name));
        }
        String canonical = SigV4.canonicalRequest(
                request.getMethod().toUpperCase(Locale.ROOT),
                AwsUri.encodeAbsolutePath(request.getRequestURI()),
                query.canonical(SIGNATURE_PARAM),
                signedHeaders,
                values,
                payloadHash);
        String expected = SigV4.signature(secret, amzDate, credential.date(), credential.region(), canonical);
        if (!SigV4.sameSignature(expected, presented)) {
            throw new S3Exception(403, "SignatureDoesNotMatch", "签名不匹配");
        }
    }

    public static String payloadHash(HttpServletRequest request, QueryParameters query) {
        boolean headerAuth = request.getHeader("Authorization") != null && !request.getHeader("Authorization").isBlank();
        return payloadHash(request, query, headerAuth);
    }

    private static String payloadHash(HttpServletRequest request, QueryParameters query, boolean headerAuth) {
        String header = request.getHeader("x-amz-content-sha256");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String fromQuery = query.first("X-Amz-Content-Sha256");
        if (fromQuery != null && !fromQuery.isBlank()) {
            return fromQuery.trim();
        }
        if (!headerAuth) {
            return "UNSIGNED-PAYLOAD";
        }
        throw new S3Exception(400, "InvalidRequest", "缺少 x-amz-content-sha256");
    }

    private static String headerValues(HttpServletRequest request, String name) {
        Enumeration<String> values = request.getHeaders(name);
        if (values == null || !values.hasMoreElements()) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "缺少签名头 " + name);
        }
        List<String> all = new ArrayList<>();
        while (values.hasMoreElements()) {
            all.add(values.nextElement());
        }
        return String.join(",", all);
    }

    private static Credential credential(String text) {
        String[] parts = text.split("/", -1);
        if (parts.length != 5 || parts[0].isBlank()) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "Credential 格式不正确");
        }
        return new Credential(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    private static Instant parseDate(String value) {
        try {
            return OffsetDateTime.parse(value, AMZ_DATE).toInstant();
        } catch (DateTimeParseException ex) {
            throw new S3Exception(400, "AuthorizationHeaderMalformed", "x-amz-date 无法解析");
        }
    }

    private static long parseExpires(String value) {
        if (value == null || value.isBlank()) {
            throw new S3Exception(400, "AuthorizationQueryParametersError", "缺少 X-Amz-Expires");
        }
        try {
            long expires = Long.parseLong(value);
            if (expires <= 0 || expires > 604800) {
                throw new S3Exception(400, "AuthorizationQueryParametersError", "X-Amz-Expires 超出范围");
            }
            return expires;
        } catch (NumberFormatException ex) {
            throw new S3Exception(400, "AuthorizationQueryParametersError", "X-Amz-Expires 不是数字");
        }
    }

    private static String firstNonBlank(String left, String right) {
        if (left != null && !left.isBlank()) {
            return left.trim();
        }
        if (right != null && !right.isBlank()) {
            return right.trim();
        }
        return null;
    }

    private record Credential(String accessKey, String date, String region, String service, String terminator) {
    }
}
