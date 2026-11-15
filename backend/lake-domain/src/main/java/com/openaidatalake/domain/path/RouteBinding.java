package com.openaidatalake.domain.path;

import com.openaidatalake.domain.storage.StorageProvider;

public record RouteBinding(
        Long pathId,
        String bucket,
        String keyPrefix,
        Long backendId,
        String endpoint,
        String region,
        StorageProvider provider,
        boolean pathStyle,
        String accessKey,
        String secretKey,
        String physicalBucket,
        String physicalPrefix,
        boolean backendEnabled
) {
}
