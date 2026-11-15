package com.openaidatalake.console.s3;

import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
public class DirectoryLister {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAX_DIRECTORIES = 200;
    private static final int MAX_BUCKETS = 20;
    private static final int MAX_CHILD_LISTS = 30;
    private static final int MAX_PAGES = 3;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public List<String> list(String endpoint, StorageProfile profile, String accessKey, String secret) {
        Instant deadline = Instant.now().plusSeconds(12);
        byte[] bucketsBody = get(listBucketsUri(endpoint), profile.region(), accessKey, secret, deadline);
        List<String> buckets = S3ListXml.bucketNames(bucketsBody);
        TreeSet<String> found = new TreeSet<>();
        int expanded = 0;
        for (String bucket : buckets) {
            if (bucket == null || bucket.isBlank() || found.size() >= MAX_DIRECTORIES || Instant.now().isAfter(deadline)) {
                break;
            }
            found.add(bucket);
            if (expanded >= MAX_BUCKETS) {
                continue;
            }
            expanded++;
            try {
                collect(endpoint, profile, accessKey, secret, bucket, "", 0, found, deadline);
            } catch (ProbeException ex) {
                // 桶能列出来，但某个前缀没有权限时仍保留已得到的目录
            }
        }
        return List.copyOf(found);
    }

    private void collect(String endpoint,
                         StorageProfile profile,
                         String accessKey,
                         String secret,
                         String bucket,
                         String prefix,
                         int depth,
                         TreeSet<String> found,
                         Instant deadline) {
        if (depth >= 2 || found.size() >= MAX_DIRECTORIES || Instant.now().isAfter(deadline)) {
            return;
        }
        String token = null;
        int pages = 0;
        int children = 0;
        List<String> nested = new ArrayList<>();
        do {
            byte[] body = get(listObjectsUri(endpoint, profile.pathStyle(), bucket, prefix, token), profile.region(), accessKey, secret, deadline);
            S3ListXml.PrefixPage page = S3ListXml.prefixes(body);
            for (String child : page.prefixes()) {
                if (found.size() >= MAX_DIRECTORIES) {
                    return;
                }
                String bare = strip(child);
                if (bare.isEmpty()) {
                    continue;
                }
                found.add(bucket + "/" + bare);
                if (depth + 1 < 2) {
                    nested.add(bare + "/");
                }
            }
            token = page.nextToken();
            pages++;
        } while (token != null && !token.isBlank() && pages < MAX_PAGES && found.size() < MAX_DIRECTORIES && Instant.now().isBefore(deadline));
        for (String childPrefix : nested) {
            if (children >= MAX_CHILD_LISTS || found.size() >= MAX_DIRECTORIES || Instant.now().isAfter(deadline)) {
                return;
            }
            children++;
            collect(endpoint, profile, accessKey, secret, bucket, childPrefix, depth + 1, found, deadline);
        }
    }

    private byte[] get(URI uri, String region, String accessKey, String secret, Instant deadline) {
        Duration left = Duration.between(Instant.now(), deadline);
        if (left.isZero() || left.isNegative()) {
            throw new ProbeException("列出目录超时");
        }
        Duration timeout = left.compareTo(Duration.ofSeconds(8)) < 0 ? left : Duration.ofSeconds(8);
        String amzDate = AMZ_DATE.format(Instant.now());
        String scopeDate = amzDate.substring(0, 8);
        String host = uri.getHost();
        if (uri.getPort() != -1) {
            host = host + ":" + uri.getPort();
        }
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-content-sha256", S3Signer.EMPTY_SHA256);
        headers.put("x-amz-date", amzDate);
        String rawPath = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String rawQuery = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        String canonical = S3Signer.canonicalRequest(
                "GET", rawPath, rawQuery, List.copyOf(headers.keySet()), headers, S3Signer.EMPTY_SHA256);
        String signature = S3Signer.signature(secret, amzDate, scopeDate, region, canonical);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET()
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", S3Signer.EMPTY_SHA256)
                .header("Authorization", S3Signer.authorization(
                        accessKey, scopeDate, region, String.join(";", headers.keySet()), signature));
        try {
            HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body() == null ? new byte[0] : response.body();
            if (body.length > 1_000_000) {
                throw new ProbeException("对象存储返回的目录过大");
            }
            if (response.statusCode() / 100 != 2) {
                String message = S3ListXml.errorMessage(body);
                if (message == null || message.isBlank()) {
                    message = "对象存储拒绝了连接（" + response.statusCode() + "）";
                }
                throw new ProbeException(message);
            }
            return body;
        } catch (ProbeException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ProbeException("连接 Endpoint 超时");
        } catch (UnknownHostException ex) {
            throw new ProbeException("无法解析 Endpoint");
        } catch (ConnectException ex) {
            throw new ProbeException("无法连接到 Endpoint");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProbeException("列出目录已中断");
        } catch (Exception ex) {
            throw new ProbeException("无法连接到 Endpoint");
        }
    }

    private static URI listBucketsUri(String endpoint) {
        URI base = URI.create(endpoint);
        return URI.create(origin(base) + "/");
    }

    private static URI listObjectsUri(String endpoint, boolean pathStyle, String bucket, String prefix, String token) {
        URI base = URI.create(endpoint);
        String host = pathStyle ? base.getHost() : bucket + "." + base.getHost();
        String path = pathStyle ? "/" + S3Signer.encode(bucket) : "/";
        StringBuilder url = new StringBuilder();
        url.append(base.getScheme()).append("://").append(host);
        if (base.getPort() != -1) {
            url.append(':').append(base.getPort());
        }
        url.append(path).append('?').append(query(prefix, token));
        return URI.create(url.toString());
    }

    private static String query(String prefix, String token) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("delimiter", "/");
        params.put("encoding-type", "url");
        params.put("list-type", "2");
        params.put("max-keys", "100");
        if (prefix != null && !prefix.isEmpty()) {
            params.put("prefix", prefix);
        }
        if (token != null && !token.isEmpty()) {
            params.put("continuation-token", token);
        }
        StringBuilder query = new StringBuilder();
        for (var entry : params.entrySet()) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(S3Signer.encode(entry.getKey())).append('=').append(S3Signer.encode(entry.getValue()));
        }
        return query.toString();
    }

    private static String origin(URI base) {
        StringBuilder origin = new StringBuilder();
        origin.append(base.getScheme()).append("://").append(base.getHost());
        if (base.getPort() != -1) {
            origin.append(':').append(base.getPort());
        }
        return origin.toString();
    }

    private static String strip(String prefix) {
        String value = prefix;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
