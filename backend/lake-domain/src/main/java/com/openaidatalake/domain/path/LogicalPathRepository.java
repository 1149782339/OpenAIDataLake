package com.openaidatalake.domain.path;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LogicalPathRepository extends JpaRepository<LogicalPath, Long> {

    Optional<LogicalPath> findByBucketAndKeyPrefix(String bucket, String keyPrefix);

    boolean existsByBucket(String bucket);

    List<LogicalPath> findAllByOrderByBucketAscKeyPrefixAsc();

    @Query("""
            select new com.openaidatalake.domain.path.RouteBinding(
                p.id, p.bucket, p.keyPrefix,
                b.id, b.endpoint, b.region, b.provider, b.pathStyle,
                b.accessKey, b.secretKey,
                r.physicalBucket, r.physicalPrefix, b.enabled)
            from LogicalPath p, PathRoute r, StorageBackend b
            where p.id = r.pathId and r.backendId = b.id and p.bucket = :bucket
            """)
    List<RouteBinding> findRoutes(@Param("bucket") String bucket);
}
