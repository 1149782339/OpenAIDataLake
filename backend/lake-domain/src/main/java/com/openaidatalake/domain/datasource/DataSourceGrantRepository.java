package com.openaidatalake.domain.datasource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceGrantRepository extends JpaRepository<DataSourceGrant, DataSourceGrantKey> {

    List<DataSourceGrant> findByIdSourceId(Long sourceId);

    List<DataSourceGrant> findByIdUserId(Long userId);
}
