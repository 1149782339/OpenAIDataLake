package com.openaidatalake.console.datasource;

import com.openaidatalake.console.web.ApiException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class ClickHouseHttp {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ClickHouseHttp() {
    }

    static void probe(String host, int port, String user, String password, String database) {
        query(host, port, user, password, database, "SELECT 1");
    }

    static List<String> tables(String host, int port, String user, String password, String database) {
        String sql = "SELECT name FROM system.tables WHERE database = " + quote(database)
                + " AND name NOT LIKE '.%' ORDER BY name FORMAT TabSeparated";
        String body = query(host, port, user, password, database, sql);
        List<String> names = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (!line.isBlank()) {
                names.add(line.trim());
            }
        }
        return List.copyOf(names);
    }

    private static String query(String host, int port, String user, String password, String database, String sql) {
        int[] ports = fallbackPorts(port);
        IOException last = null;
        for (int index = 0; index < ports.length; index++) {
            try {
                return send(host, ports[index], user, password, database, sql);
            } catch (IOException ex) {
                last = ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ApiException.bad("连接 ClickHouse 被中断");
            }
        }
        throw ApiException.bad(SecretText.limit("连接失败：" + (last == null ? "未知错误" : last.getMessage())));
    }

    private static String send(String host, int port, String user, String password, String database, String sql)
            throws IOException, InterruptedException {
        String scheme = port == 8443 || port == 9440 ? "https" : "http";
        URI uri = URI.create(scheme + "://" + host + ":" + port + "/?database="
                + URLEncoder.encode(database, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("X-ClickHouse-User", user)
                .header("X-ClickHouse-Key", password)
                .POST(HttpRequest.BodyPublishers.ofString(sql))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw ApiException.bad(SecretText.limit("连接失败：" + SecretText.redact(response.body(), password)));
        }
        return response.body();
    }

    private static int[] fallbackPorts(int port) {
        if (port == 9000) {
            return new int[] {9000, 8123};
        }
        if (port == 9440) {
            return new int[] {9440, 8443};
        }
        return new int[] {port};
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
