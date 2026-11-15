package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 按语句里出现的数据源名称核对当前用户的授权。
 * 没有登记的目录（例如 memory）不在这里拦截。
 */
public final class SourceAccess {

    public record SourceRule(long id, String name, boolean clickHouse) {
    }

    public record GrantRule(boolean writable) {
    }

    private SourceAccess() {
    }

    public static void check(SqlGate.Verdict verdict,
                             Map<String, SourceRule> sourcesByLowerName,
                             Map<Long, GrantRule> grantsBySourceId) {
        Set<String> writes = new HashSet<>();
        for (String catalog : verdict.writeCatalogs()) {
            writes.add(catalog.toLowerCase(Locale.ROOT));
        }
        for (String catalog : verdict.readCatalogs()) {
            String key = catalog.toLowerCase(Locale.ROOT);
            if (writes.contains(key)) {
                continue;
            }
            SourceRule source = sourcesByLowerName.get(key);
            if (source == null) {
                continue;
            }
            if (!grantsBySourceId.containsKey(source.id())) {
                throw ApiException.bad("没有 " + source.name() + " 的读权限");
            }
        }
        for (String catalog : verdict.writeCatalogs()) {
            SourceRule source = sourcesByLowerName.get(catalog.toLowerCase(Locale.ROOT));
            if (source == null) {
                continue;
            }
            GrantRule grant = grantsBySourceId.get(source.id());
            if (grant == null || !grant.writable()) {
                throw ApiException.bad("没有 " + source.name() + " 的写权限");
            }
            if (source.clickHouse()) {
                throw ApiException.bad("ClickHouse 数据源不支持写入");
            }
        }
    }
}
