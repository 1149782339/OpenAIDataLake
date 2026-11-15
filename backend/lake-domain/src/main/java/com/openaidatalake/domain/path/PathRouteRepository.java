package com.openaidatalake.domain.path;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PathRouteRepository extends JpaRepository<PathRoute, Long> {

    long countByBackendId(Long backendId);
}
