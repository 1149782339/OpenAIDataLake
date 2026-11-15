package com.openaidatalake.domain.datasource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DataSourceGrantKey implements Serializable {

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "user_id")
    private Long userId;

    protected DataSourceGrantKey() {
    }

    public DataSourceGrantKey(Long sourceId, Long userId) {
        this.sourceId = sourceId;
        this.userId = userId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DataSourceGrantKey key)) {
            return false;
        }
        return Objects.equals(sourceId, key.sourceId) && Objects.equals(userId, key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, userId);
    }
}
