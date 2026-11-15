package com.openaidatalake.console.service;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.crypto.AesTextCipher;
import com.openaidatalake.domain.path.PathRouteRepository;
import com.openaidatalake.domain.storage.StorageBackend;
import com.openaidatalake.domain.storage.StorageBackendRepository;
import com.openaidatalake.domain.storage.StorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
public class BackendService {

    private final StorageBackendRepository backends;
    private final PathRouteRepository routes;
    private final AesTextCipher cipher;

    public BackendService(StorageBackendRepository backends, PathRouteRepository routes, AesTextCipher cipher) {
        this.backends = backends;
        this.routes = routes;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public List<BackendView> list() {
        return backends.findAllByOrderByIdAsc().stream().map(BackendView::from).toList();
    }

    @Transactional
    public BackendView create(BackendCommand command) {
        if (backends.findByName(command.name()).isPresent()) {
            throw ApiException.conflict("数据源名称已存在");
        }
        StorageBackend backend = new StorageBackend();
        apply(backend, command, true);
        return BackendView.from(backends.save(backend));
    }

    @Transactional
    public BackendView update(long id, BackendCommand command) {
        StorageBackend backend = backends.findById(id).orElseThrow(() -> ApiException.notFound("数据源不存在"));
        backends.findByName(command.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.conflict("数据源名称已存在");
                });
        apply(backend, command, false);
        return BackendView.from(backend);
    }

    @Transactional
    public void delete(long id) {
        if (!backends.existsById(id)) {
            throw ApiException.notFound("数据源不存在");
        }
        if (routes.countByBackendId(id) > 0) {
            throw ApiException.conflict("仍有路径指向这个数据源");
        }
        backends.deleteById(id);
    }

    private void apply(StorageBackend backend, BackendCommand command, boolean creating) {
        backend.setName(required(command.name(), "名称不能为空"));
        backend.setProvider(command.provider());
        backend.setEndpoint(normalizeEndpoint(command.endpoint()));
        backend.setRegion(required(command.region(), "区域不能为空"));
        boolean pathStyle = command.pathStyle() != null
                ? command.pathStyle()
                : command.provider() == StorageProvider.MINIO;
        backend.setPathStyle(pathStyle);
        backend.setAccessKey(required(command.accessKey(), "Access Key 不能为空"));
        if (command.secretKey() != null && !command.secretKey().isBlank()) {
            backend.setSecretKey(cipher.encrypt(command.secretKey()));
        } else if (creating) {
            throw ApiException.bad("Secret Key 不能为空");
        }
        backend.setEnabled(command.enabled() == null || command.enabled());
    }

    static String normalizeEndpoint(String endpoint) {
        String value = required(endpoint, "地址不能为空");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw ApiException.bad("地址不合法");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw ApiException.bad("地址必须是 http 或 https");
        }
        if (uri.getHost() == null || uri.getRawQuery() != null || uri.getFragment() != null) {
            throw ApiException.bad("地址不合法");
        }
        if (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath())) {
            throw ApiException.bad("地址不能带路径");
        }
        return value;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.bad(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > 512) {
            throw ApiException.bad("字段过长");
        }
        return trimmed;
    }

    public record BackendCommand(
            String name,
            StorageProvider provider,
            String endpoint,
            String region,
            Boolean pathStyle,
            String accessKey,
            String secretKey,
            Boolean enabled
    ) {
    }

    public record BackendView(
            long id,
            String name,
            StorageProvider provider,
            String endpoint,
            String region,
            boolean pathStyle,
            String accessKey,
            boolean enabled,
            Instant createdAt
    ) {
        static BackendView from(StorageBackend backend) {
            return new BackendView(
                    backend.getId(),
                    backend.getName(),
                    backend.getProvider(),
                    backend.getEndpoint(),
                    backend.getRegion(),
                    backend.isPathStyle(),
                    backend.getAccessKey(),
                    backend.isEnabled(),
                    backend.getCreatedAt());
        }
    }
}
