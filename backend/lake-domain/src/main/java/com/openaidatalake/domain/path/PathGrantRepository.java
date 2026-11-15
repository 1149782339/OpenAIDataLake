package com.openaidatalake.domain.path;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PathGrantRepository extends JpaRepository<PathGrant, PathGrantKey> {

    List<PathGrant> findByIdPathId(Long pathId);

    @Query("""
            select new com.openaidatalake.domain.path.GrantBinding(
                p.bucket, p.keyPrefix, g.canList, g.canRead, g.canWrite, g.canDelete)
            from PathGrant g, LogicalPath p
            where g.id.pathId = p.id and g.id.userId = :userId and p.bucket = :bucket
            """)
    List<GrantBinding> findGrants(@Param("userId") long userId, @Param("bucket") String bucket);

    @Query("""
            select distinct p.bucket from PathGrant g, LogicalPath p
            where g.id.pathId = p.id and g.id.userId = :userId
            order by p.bucket
            """)
    List<String> findGrantedBuckets(@Param("userId") long userId);
}
