package com.openaidatalake.domain.path;

public record GrantBinding(
        String bucket,
        String keyPrefix,
        boolean canList,
        boolean canRead,
        boolean canWrite,
        boolean canDelete
) {
}
