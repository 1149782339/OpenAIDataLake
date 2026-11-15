package com.openaidatalake.gateway.s3;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class QueryParameters {

    private final List<Param> params;

    private QueryParameters(List<Param> params) {
        this.params = params;
    }

    public static QueryParameters parse(String raw) {
        List<Param> params = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return new QueryParameters(params);
        }
        for (String part : raw.split("&", -1)) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                params.add(new Param(decode(part), ""));
            } else {
                params.add(new Param(decode(part.substring(0, eq)), decode(part.substring(eq + 1))));
            }
        }
        return new QueryParameters(params);
    }

    public static QueryParameters of(List<Param> params) {
        return new QueryParameters(new ArrayList<>(params));
    }

    public boolean has(String name) {
        return params.stream().anyMatch(param -> param.name().equals(name));
    }

    public String first(String name) {
        for (Param param : params) {
            if (param.name().equals(name)) {
                return param.value();
            }
        }
        return null;
    }

    public List<Param> params() {
        return params;
    }

    public String canonical(Set<String> exclude) {
        List<String> encoded = new ArrayList<>();
        for (Param param : params) {
            if (exclude.contains(param.name())) {
                continue;
            }
            encoded.add(AwsUri.encode(param.name()) + "=" + AwsUri.encode(param.value()));
        }
        encoded.sort(String::compareTo);
        return String.join("&", encoded);
    }

    public QueryParameters without(Set<String> names) {
        List<Param> kept = params.stream().filter(param -> !names.contains(param.name())).toList();
        return new QueryParameters(new ArrayList<>(kept));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public record Param(String name, String value) {
    }
}
