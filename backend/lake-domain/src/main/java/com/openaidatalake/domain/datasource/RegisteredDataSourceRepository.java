package com.openaidatalake.domain.datasource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegisteredDataSourceRepository extends JpaRepository<RegisteredDataSource, Long> {

    List<RegisteredDataSource> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);
}
