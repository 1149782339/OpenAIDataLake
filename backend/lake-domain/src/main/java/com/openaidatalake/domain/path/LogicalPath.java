package com.openaidatalake.domain.path;

import com.openaidatalake.domain.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "logical_paths")
public class LogicalPath extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 63)
    private String bucket;

    @Column(name = "key_prefix", nullable = false, length = 1024)
    private String keyPrefix;

    @Column(length = 255)
    private String description;

    protected LogicalPath() {
    }

    public LogicalPath(String bucket, String keyPrefix, String description) {
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
