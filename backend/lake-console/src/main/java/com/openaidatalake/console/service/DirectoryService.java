package com.openaidatalake.console.service;

import com.openaidatalake.console.s3.DirectoryLister;
import com.openaidatalake.console.s3.ProbeException;
import com.openaidatalake.console.s3.StorageProfile;
import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.storage.StorageBackend;
import com.openaidatalake.domain.storage.StorageBackendRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DirectoryService {

    private final PathService paths;
    private final BackendService backends;
    private final StorageBackendRepository backendRepository;
    private final DirectoryLister lister;

    public DirectoryService(PathService paths,
                            BackendService backends,
                            StorageBackendRepository backendRepository,
                            DirectoryLister lister) {
        this.paths = paths;
        this.backends = backends;
        this.backendRepository = backendRepository;
        this.lister = lister;
    }

    @Transactional(readOnly = true)
    public List<DirectoryItem> list() {
        var backendById = backends.list().stream().collect(java.util.stream.Collectors.toMap(BackendService.BackendView::id, view -> view));
        return paths.list().stream().map(path -> {
            String endpoint = null;
            String directory = null;
            if (path.route() != null) {
                BackendService.BackendView backend = backendById.get(path.route().backendId());
                endpoint = backend == null ? null : backend.endpoint();
                directory = display(path.route().physicalBucket(), path.route().physicalPrefix());
            }
            return new DirectoryItem(path.id(), display(path.bucket(), path.keyPrefix()), endpoint, directory);
        }).toList();
    }

    public List<String> probe(String endpoint, String accessKey, String secretKey) {
        String normalized = BackendService.normalizeEndpoint(endpoint);
        StorageProfile profile = StorageProfile.infer(URI.create(normalized));
        try {
            return lister.list(normalized, profile, accessKey.trim(), secretKey);
        } catch (ProbeException ex) {
            throw ApiException.bad(ex.getMessage());
        }
    }

    @Transactional
    public DirectoryItem create(String logicalName,
                                String endpoint,
                                String accessKey,
                                String secretKey,
                                String directory,
                                List<GrantInput> grants) {
        String[] logical = split(logicalName, "逻辑名称");
        String[] physical = split(directory, "文件目录");
        String normalized = BackendService.normalizeEndpoint(endpoint);
        StorageProfile profile = StorageProfile.infer(URI.create(normalized));
        String key = accessKey.trim();
        StorageBackend existing = backendRepository.findByEndpointAndAccessKey(normalized, key).stream().findFirst().orElse(null);
        long backendId;
        if (existing == null) {
            backendId = backends.create(new BackendService.BackendCommand(
                    uniqueName(normalized),
                    profile.provider(),
                    normalized,
                    profile.region(),
                    profile.pathStyle(),
                    key,
                    secretKey,
                    true)).id();
        } else {
            backendId = backends.update(existing.getId(), new BackendService.BackendCommand(
                    existing.getName(),
                    profile.provider(),
                    normalized,
                    profile.region(),
                    profile.pathStyle(),
                    key,
                    secretKey,
                    true)).id();
        }
        PathService.PathView path = paths.create(logical[0], logical[1], null);
        PathService.PathView.RouteView route = paths.upsertRoute(path.id(), backendId, physical[0], physical[1]);
        saveGrants(path.id(), grants);
        return new DirectoryItem(
                path.id(),
                display(path.bucket(), path.keyPrefix()),
                normalized,
                display(route.physicalBucket(), route.physicalPrefix()));
    }

    private void saveGrants(long pathId, List<GrantInput> grants) {
        if (grants == null || grants.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (GrantInput grant : grants) {
            if (grant == null) {
                continue;
            }
            if (!grant.canList() && !grant.canRead() && !grant.canWrite() && !grant.canDelete()) {
                throw ApiException.bad("权限至少要勾选一项");
            }
            if (!seen.add(grant.userId())) {
                throw ApiException.bad("同一个用户不能重复授权");
            }
            paths.upsertGrant(pathId, grant.userId(), grant.canList(), grant.canRead(), grant.canWrite(), grant.canDelete());
        }
    }

    private String uniqueName(String endpoint) {
        URI uri = URI.create(endpoint);
        String host = uri.getHost() == null ? "backend" : uri.getHost();
        String base = host + (uri.getPort() > 0 ? "-" + uri.getPort() : "");
        base = base.replaceAll("[^A-Za-z0-9._-]", "-");
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        if (base.isBlank()) {
            base = "backend";
        }
        String name = base;
        int suffix = 2;
        while (backendRepository.findByName(name).isPresent()) {
            name = base + "-" + suffix++;
        }
        return name;
    }

    private static String[] split(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.bad(label + "不能为空");
        }
        String value = raw.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            throw ApiException.bad(label + "不能为空");
        }
        int slash = value.indexOf('/');
        String bucket = slash < 0 ? value : value.substring(0, slash);
        String prefix = slash < 0 ? "" : value.substring(slash + 1);
        return new String[]{bucket, prefix};
    }

    static String display(String bucket, String prefix) {
        if (prefix == null || prefix.isBlank() || "/".equals(prefix)) {
            return bucket;
        }
        String bare = prefix;
        while (bare.endsWith("/")) {
            bare = bare.substring(0, bare.length() - 1);
        }
        return bare.isEmpty() ? bucket : bucket + "/" + bare;
    }

    public record DirectoryItem(long id, String logicalName, String endpoint, String directory) {
    }

    public record GrantInput(long userId, boolean canList, boolean canRead, boolean canWrite, boolean canDelete) {
    }
}
