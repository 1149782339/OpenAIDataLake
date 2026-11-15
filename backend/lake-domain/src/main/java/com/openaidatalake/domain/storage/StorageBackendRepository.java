package com.openaidatalake.domain.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageBackendRepository extends JpaRepository<StorageBackend, Long> {

    Optional<StorageBackend> findByName(String name);

    List<StorageBackend> findByEndpointAndAccessKey(String endpoint, String accessKey);

    List<StorageBackend> findAllByOrderByIdAsc();
}
