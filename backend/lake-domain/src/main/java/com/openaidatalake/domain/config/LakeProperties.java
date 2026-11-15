package com.openaidatalake.domain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lake")
public class LakeProperties {

    /**
     * 32 字节密钥的 Base64。用来加密 S3 Secret 和数据源密钥。
     */
    private String masterKey = "";

    private long maxUploadBytes = 1024L * 1024L * 1024L;

    private final Admin admin = new Admin();

    private final Cors cors = new Cors();

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Admin {
        private String username = "";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
