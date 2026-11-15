package com.openaidatalake.console.s3;

import com.openaidatalake.domain.storage.StorageProvider;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 页面只收集 Endpoint 和密钥。区域、厂商、路径风格按地址推断。
 */
public record StorageProfile(StorageProvider provider, String region, boolean pathStyle) {

    private static final Pattern OSS_REGION = Pattern.compile("oss-([a-z0-9-]+)\\.aliyuncs\\.com");
    private static final Pattern AWS_DOT = Pattern.compile("(?:^|\\.)s3\\.([a-z0-9-]+)\\.amazonaws\\.com$");
    private static final Pattern AWS_DASH = Pattern.compile("(?:^|\\.)s3-([a-z0-9-]+)\\.amazonaws\\.com$");

    public static StorageProfile infer(URI endpoint) {
        String host = endpoint.getHost() == null ? "" : endpoint.getHost().toLowerCase(Locale.ROOT);
        Matcher oss = OSS_REGION.matcher(host);
        if (oss.find()) {
            return new StorageProfile(StorageProvider.OSS, oss.group(1), false);
        }
        if (host.endsWith(".aliyuncs.com") || host.contains(".oss-")) {
            return new StorageProfile(StorageProvider.OSS, "cn-hangzhou", false);
        }
        if (host.equals("s3.amazonaws.com") || host.endsWith(".s3.amazonaws.com")) {
            return new StorageProfile(StorageProvider.AWS, "us-east-1", false);
        }
        Matcher awsDot = AWS_DOT.matcher(host);
        if (awsDot.find()) {
            return new StorageProfile(StorageProvider.AWS, awsDot.group(1), false);
        }
        Matcher awsDash = AWS_DASH.matcher(host);
        if (awsDash.find()) {
            return new StorageProfile(StorageProvider.AWS, awsDash.group(1), false);
        }
        if (host.endsWith(".amazonaws.com")) {
            return new StorageProfile(StorageProvider.AWS, "us-east-1", false);
        }
        return new StorageProfile(StorageProvider.MINIO, "us-east-1", true);
    }
}
