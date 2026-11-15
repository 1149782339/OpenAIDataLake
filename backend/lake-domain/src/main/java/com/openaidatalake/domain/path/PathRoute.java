package com.openaidatalake.domain.path;

import com.openaidatalake.domain.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "path_routes")
public class PathRoute extends CreatedEntity implements Persistable<Long> {

    @Transient
    private boolean fresh = true;

    @Id
    @Column(name = "path_id")
    private Long pathId;

    @Column(name = "backend_id", nullable = false)
    private Long backendId;

    @Column(name = "physical_bucket", nullable = false, length = 63)
    private String physicalBucket;

    @Column(name = "physical_prefix", nullable = false, length = 1024)
    private String physicalPrefix;

    protected PathRoute() {
    }

    public PathRoute(Long pathId, Long backendId, String physicalBucket, String physicalPrefix) {
        this.pathId = pathId;
        this.backendId = backendId;
        this.physicalBucket = physicalBucket;
        this.physicalPrefix = physicalPrefix;
    }

    public Long getPathId() {
        return pathId;
    }

    @Override
    public Long getId() {
        return pathId;
    }

    @Override
    public boolean isNew() {
        return fresh;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        fresh = false;
    }

    public Long getBackendId() {
        return backendId;
    }

    public void setBackendId(Long backendId) {
        this.backendId = backendId;
    }

    public String getPhysicalBucket() {
        return physicalBucket;
    }

    public void setPhysicalBucket(String physicalBucket) {
        this.physicalBucket = physicalBucket;
    }

    public String getPhysicalPrefix() {
        return physicalPrefix;
    }

    public void setPhysicalPrefix(String physicalPrefix) {
        this.physicalPrefix = physicalPrefix;
    }
}
