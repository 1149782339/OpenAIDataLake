package com.openaidatalake.domain.datasource;

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
@Table(name = "data_source_grants")
public class DataSourceGrant extends CreatedEntity implements Persistable<DataSourceGrantKey> {

    @Transient
    private boolean fresh = true;

    @EmbeddedId
    private DataSourceGrantKey id;

    @Column(nullable = false)
    private boolean writable;

    protected DataSourceGrant() {
    }

    public DataSourceGrant(DataSourceGrantKey id, boolean writable) {
        this.id = id;
        this.writable = writable;
    }

    @Override
    public DataSourceGrantKey getId() {
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

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }
}
