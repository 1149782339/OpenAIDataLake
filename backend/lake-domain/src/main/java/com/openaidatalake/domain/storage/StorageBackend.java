package com.openaidatalake.domain.storage;

import com.openaidatalake.domain.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "storage_backends")
public class StorageBackend extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StorageProvider provider;

    @Column(nullable = false, length = 512)
    private String endpoint;

    @Column(nullable = false, length = 64)
    private String region;

    @Column(name = "path_style", nullable = false)
    private boolean pathStyle;

    @Column(name = "access_key", nullable = false, length = 256)
    private String accessKey;

    @Column(name = "secret_key", nullable = false, length = 512)
    private String secretKey;

    @Column(nullable = false)
    private boolean enabled = true;

    public StorageBackend() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(boolean pathStyle) {
        this.pathStyle = pathStyle;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
