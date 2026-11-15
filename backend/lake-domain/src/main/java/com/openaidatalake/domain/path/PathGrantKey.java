package com.openaidatalake.domain.path;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PathGrantKey implements Serializable {

    @Column(name = "path_id")
    private Long pathId;

    @Column(name = "user_id")
    private Long userId;

    protected PathGrantKey() {
    }

    public PathGrantKey(Long pathId, Long userId) {
        this.pathId = pathId;
        this.userId = userId;
    }

    public Long getPathId() {
        return pathId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PathGrantKey key)) {
            return false;
        }
        return Objects.equals(pathId, key.pathId) && Objects.equals(userId, key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathId, userId);
    }
}
