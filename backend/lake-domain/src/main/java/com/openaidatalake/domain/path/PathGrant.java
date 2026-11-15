package com.openaidatalake.domain.path;

import com.openaidatalake.domain.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "path_grants")
public class PathGrant extends CreatedEntity implements Persistable<PathGrantKey> {

    @Transient
    private boolean fresh = true;

    @EmbeddedId
    private PathGrantKey id;

    @Column(name = "can_list", nullable = false)
    private boolean canList;

    @Column(name = "can_read", nullable = false)
    private boolean canRead;

    @Column(name = "can_write", nullable = false)
    private boolean canWrite;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;

    protected PathGrant() {
    }

    public PathGrant(PathGrantKey id, boolean canList, boolean canRead, boolean canWrite, boolean canDelete) {
        this.id = id;
        this.canList = canList;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.canDelete = canDelete;
    }

    @Override
    public PathGrantKey getId() {
        return id;
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

    public boolean isCanList() {
        return canList;
    }

    public void setCanList(boolean canList) {
        this.canList = canList;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}
