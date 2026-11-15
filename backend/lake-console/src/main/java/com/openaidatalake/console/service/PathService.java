package com.openaidatalake.console.service;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.path.LogicalPath;
import com.openaidatalake.domain.path.LogicalPathRepository;
import com.openaidatalake.domain.path.PathGrant;
import com.openaidatalake.domain.path.PathGrantKey;
import com.openaidatalake.domain.path.PathGrantRepository;
import com.openaidatalake.domain.path.PathPrefixes;
import com.openaidatalake.domain.path.PathRoute;
import com.openaidatalake.domain.path.PathRouteRepository;
import com.openaidatalake.domain.path.RouteBinding;
import com.openaidatalake.domain.storage.StorageBackend;
import com.openaidatalake.domain.storage.StorageBackendRepository;
import com.openaidatalake.domain.user.UserAccount;
import com.openaidatalake.domain.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PathService {

    private final LogicalPathRepository paths;
    private final PathRouteRepository routes;
    private final PathGrantRepository grants;
    private final StorageBackendRepository backends;
    private final UserAccountRepository users;

    public PathService(LogicalPathRepository paths,
                       PathRouteRepository routes,
                       PathGrantRepository grants,
                       StorageBackendRepository backends,
                       UserAccountRepository users) {
        this.paths = paths;
        this.routes = routes;
        this.grants = grants;
        this.backends = backends;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<PathView> list() {
        Map<Long, StorageBackend> backendById = backends.findAll().stream()
                .collect(Collectors.toMap(StorageBackend::getId, Function.identity()));
        Map<Long, PathRoute> routeByPath = routes.findAll().stream()
                .collect(Collectors.toMap(PathRoute::getPathId, Function.identity()));
        return paths.findAllByOrderByBucketAscKeyPrefixAsc().stream()
                .map(path -> PathView.from(path, routeByPath.get(path.getId()), backendById))
                .toList();
    }

    @Transactional
    public PathView create(String bucket, String keyPrefix, String description) {
        String normalizedBucket = bucket(bucket);
        String prefix = prefix(keyPrefix);
        if (paths.findByBucketAndKeyPrefix(normalizedBucket, prefix).isPresent()) {
            throw ApiException.conflict("路径已存在");
        }
        String text = description == null || description.isBlank() ? null : description.trim();
        if (text != null && text.length() > 255) {
            throw ApiException.bad("说明过长");
        }
        LogicalPath saved = paths.save(new LogicalPath(normalizedBucket, prefix, text));
        return PathView.from(saved, null, Map.of());
    }

    @Transactional
    public void delete(long id) {
        if (!paths.existsById(id)) {
            throw ApiException.notFound("路径不存在");
        }
        paths.deleteById(id);
    }

    @Transactional
    public PathView.RouteView upsertRoute(long pathId, long backendId, String physicalBucket, String physicalPrefix) {
        LogicalPath path = paths.findById(pathId).orElseThrow(() -> ApiException.notFound("路径不存在"));
        StorageBackend backend = backends.findById(backendId).orElseThrow(() -> ApiException.notFound("数据源不存在"));
        String bucketName = bucket(physicalBucket);
        String prefix = prefix(physicalPrefix);
        for (RouteBinding other : paths.findRoutes(path.getBucket())) {
            if (!other.pathId().equals(pathId) && PathPrefixes.overlaps(other.keyPrefix(), path.getKeyPrefix())) {
                throw ApiException.conflict("路径路由不能互相覆盖");
            }
        }
        PathRoute route = routes.findById(pathId).orElseGet(() -> new PathRoute(pathId, backend.getId(), bucketName, prefix));
        route.setBackendId(backend.getId());
        route.setPhysicalBucket(bucketName);
        route.setPhysicalPrefix(prefix);
        routes.save(route);
        return new PathView.RouteView(backend.getId(), backend.getName(), bucketName, prefix);
    }

    @Transactional
    public void deleteRoute(long pathId) {
        if (!routes.existsById(pathId)) {
            throw ApiException.notFound("这条路径还没有路由");
        }
        routes.deleteById(pathId);
    }

    @Transactional(readOnly = true)
    public List<GrantView> listGrants(long pathId) {
        if (!paths.existsById(pathId)) {
            throw ApiException.notFound("路径不存在");
        }
        Map<Long, UserAccount> userById = users.findAll().stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        return grants.findByIdPathId(pathId).stream()
                .map(grant -> GrantView.from(grant, userById.get(grant.getId().getUserId())))
                .toList();
    }

    @Transactional
    public GrantView upsertGrant(long pathId, long userId, boolean canList, boolean canRead, boolean canWrite, boolean canDelete) {
        if (!paths.existsById(pathId)) {
            throw ApiException.notFound("路径不存在");
        }
        UserAccount user = users.findById(userId).orElseThrow(() -> ApiException.notFound("用户不存在"));
        if (!canList && !canRead && !canWrite && !canDelete) {
            throw ApiException.bad("至少要有一项权限");
        }
        PathGrantKey key = new PathGrantKey(pathId, userId);
        PathGrant grant = grants.findById(key).orElseGet(() -> new PathGrant(key, canList, canRead, canWrite, canDelete));
        grant.setCanList(canList);
        grant.setCanRead(canRead);
        grant.setCanWrite(canWrite);
        grant.setCanDelete(canDelete);
        grants.save(grant);
        return GrantView.from(grant, user);
    }

    @Transactional
    public void deleteGrant(long pathId, long userId) {
        PathGrantKey key = new PathGrantKey(pathId, userId);
        if (!grants.existsById(key)) {
            throw ApiException.notFound("授权不存在");
        }
        grants.deleteById(key);
    }

    private static String bucket(String value) {
        try {
            PathPrefixes.checkBucket(value == null ? null : value.trim());
        } catch (IllegalArgumentException ex) {
            throw ApiException.bad(ex.getMessage());
        }
        return value.trim();
    }

    private static String prefix(String value) {
        try {
            return PathPrefixes.normalizePrefix(value);
        } catch (IllegalArgumentException ex) {
            throw ApiException.bad(ex.getMessage());
        }
    }

    public record PathView(
            long id,
            String bucket,
            String keyPrefix,
            String description,
            Instant createdAt,
            RouteView route
    ) {
        static PathView from(LogicalPath path, PathRoute route, Map<Long, StorageBackend> backends) {
            RouteView view = null;
            if (route != null) {
                StorageBackend backend = backends.get(route.getBackendId());
                view = new RouteView(
                        route.getBackendId(),
                        backend == null ? null : backend.getName(),
                        route.getPhysicalBucket(),
                        route.getPhysicalPrefix());
            }
            return new PathView(path.getId(), path.getBucket(), path.getKeyPrefix(), path.getDescription(), path.getCreatedAt(), view);
        }

        public record RouteView(long backendId, String backendName, String physicalBucket, String physicalPrefix) {
        }
    }

    public record GrantView(
            long pathId,
            long userId,
            String username,
            boolean canList,
            boolean canRead,
            boolean canWrite,
            boolean canDelete
    ) {
        static GrantView from(PathGrant grant, UserAccount user) {
            return new GrantView(
                    grant.getId().getPathId(),
                    grant.getId().getUserId(),
                    user == null ? null : user.getUsername(),
                    grant.isCanList(),
                    grant.isCanRead(),
                    grant.isCanWrite(),
                    grant.isCanDelete());
        }
    }
}
