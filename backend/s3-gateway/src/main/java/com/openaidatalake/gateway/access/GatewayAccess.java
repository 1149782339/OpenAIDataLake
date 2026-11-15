package com.openaidatalake.gateway.access;

import com.openaidatalake.domain.crypto.AesTextCipher;
import com.openaidatalake.domain.path.GrantBinding;
import com.openaidatalake.domain.path.LogicalPathRepository;
import com.openaidatalake.domain.path.PathGrantRepository;
import com.openaidatalake.domain.path.PathPrefixes;
import com.openaidatalake.domain.path.RouteBinding;
import com.openaidatalake.domain.user.UserAccessKeyRepository;
import com.openaidatalake.domain.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class GatewayAccess {

    private final UserAccessKeyRepository keys;
    private final UserAccountRepository users;
    private final LogicalPathRepository paths;
    private final PathGrantRepository grants;
    private final AesTextCipher cipher;

    public GatewayAccess(UserAccessKeyRepository keys,
                         UserAccountRepository users,
                         LogicalPathRepository paths,
                         PathGrantRepository grants,
                         AesTextCipher cipher) {
        this.keys = keys;
        this.users = users;
        this.paths = paths;
        this.grants = grants;
        this.cipher = cipher;
    }

    public Auth authenticate(String accessKey) {
        var key = keys.findByAccessKey(accessKey).orElse(null);
        if (key == null || !key.isEnabled()) {
            return Auth.notFound();
        }
        var user = users.findById(key.getUserId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return Auth.userDisabled();
        }
        return Auth.ok(new Caller(user.getId(), cipher.decrypt(key.getSecretKey())));
    }

    public List<String> buckets(long userId) {
        return grants.findGrantedBuckets(userId);
    }

    public BucketCheck bucket(long userId, String bucket) {
        if (!paths.existsByBucket(bucket)) {
            return BucketCheck.NO_BUCKET;
        }
        if (grants.findGrants(userId, bucket).isEmpty()) {
            return BucketCheck.DENIED;
        }
        return BucketCheck.OK;
    }

    public ObjectAccess object(long userId, String bucket, String key, Action action) {
        if (!paths.existsByBucket(bucket)) {
            return ObjectAccess.missingBucket();
        }
        List<GrantBinding> grantList = grants.findGrants(userId, bucket);
        if (!allows(grantList, key, action)) {
            return ObjectAccess.denied();
        }
        RouteBinding route = longest(enabledRoutes(bucket), key);
        if (route == null) {
            return ObjectAccess.missingKey();
        }
        return ObjectAccess.ok(target(route, key));
    }

    public ListAccess list(long userId, String bucket, String prefix, String delimiter) {
        if (!paths.existsByBucket(bucket)) {
            return ListAccess.missingBucket();
        }
        List<GrantBinding> grantList = grants.findGrants(userId, bucket);
        if (grantList.stream().noneMatch(GrantBinding::canList)) {
            return ListAccess.denied();
        }
        List<RouteBinding> visible = active(bucket, grantList);
        RouteBinding route = longest(visible, prefix);
        if (route != null) {
            return ListAccess.forward(target(route, prefix));
        }
        List<String> prefixes = visible.stream().map(RouteBinding::keyPrefix).toList();
        return ListAccess.virtual(group(prefix, delimiter, prefixes));
    }

    private List<RouteBinding> enabledRoutes(String bucket) {
        return paths.findRoutes(bucket).stream().filter(RouteBinding::backendEnabled).toList();
    }

    private List<RouteBinding> active(String bucket, List<GrantBinding> grantList) {
        return paths.findRoutes(bucket).stream()
                .filter(RouteBinding::backendEnabled)
                .filter(route -> allows(grantList, route.keyPrefix(), Action.LIST) || allows(grantList, strip(route.keyPrefix()), Action.LIST))
                .toList();
    }

    private boolean allows(List<GrantBinding> grantList, String path, Action action) {
        for (GrantBinding grant : grantList) {
            if (!PathPrefixes.covers(grant.keyPrefix(), path)) {
                continue;
            }
            boolean allowed = switch (action) {
                case LIST -> grant.canList();
                case READ -> grant.canRead();
                case WRITE -> grant.canWrite();
                case DELETE -> grant.canDelete();
            };
            if (allowed) {
                return true;
            }
        }
        return false;
    }

    private RouteBinding longest(List<RouteBinding> routes, String path) {
        return routes.stream()
                .filter(route -> PathPrefixes.covers(route.keyPrefix(), path))
                .max(Comparator.comparingInt(route -> route.keyPrefix().length()))
                .orElse(null);
    }

    private Target target(RouteBinding route, String logicalKey) {
        return new Target(
                route.endpoint(),
                route.region(),
                route.pathStyle(),
                route.accessKey(),
                cipher.decrypt(route.secretKey()),
                route.physicalBucket(),
                PathPrefixes.join(route.physicalPrefix(), route.keyPrefix(), logicalKey),
                route.keyPrefix(),
                route.physicalPrefix());
    }

    static List<String> group(String listPrefix, String delimiter, List<String> routePrefixes) {
        TreeSet<String> grouped = new TreeSet<>();
        String prefix = listPrefix == null ? "" : listPrefix;
        String delim = delimiter == null || delimiter.isEmpty() ? "/" : delimiter;
        for (String routePrefix : routePrefixes) {
            if (!routePrefix.startsWith(prefix)) {
                continue;
            }
            String rest = routePrefix.substring(prefix.length());
            int index = rest.indexOf(delim);
            grouped.add(index >= 0 ? prefix + rest.substring(0, index + delim.length()) : prefix + rest);
        }
        return List.copyOf(grouped);
    }

    private static String strip(String prefix) {
        if (prefix.endsWith("/") && prefix.length() > 1) {
            return prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    public enum Action {
        LIST, READ, WRITE, DELETE
    }

    public enum BucketCheck {
        OK, NO_BUCKET, DENIED
    }

    public record Caller(long userId, String secret) {
    }

    public record Auth(Caller caller, boolean unknown, boolean disabled) {
        static Auth ok(Caller caller) {
            return new Auth(caller, false, false);
        }

        static Auth notFound() {
            return new Auth(null, true, false);
        }

        static Auth userDisabled() {
            return new Auth(null, false, true);
        }
    }

    public record Target(
            String endpoint,
            String region,
            boolean pathStyle,
            String accessKey,
            String secret,
            String physicalBucket,
            String physicalKey,
            String logicalPrefix,
            String physicalPrefix
    ) {
    }

    public record ObjectAccess(Kind kind, Target target) {
        public enum Kind { OK, NO_BUCKET, DENIED, NO_SUCH_KEY }

        static ObjectAccess ok(Target target) {
            return new ObjectAccess(Kind.OK, target);
        }

        static ObjectAccess missingBucket() {
            return new ObjectAccess(Kind.NO_BUCKET, null);
        }

        static ObjectAccess denied() {
            return new ObjectAccess(Kind.DENIED, null);
        }

        static ObjectAccess missingKey() {
            return new ObjectAccess(Kind.NO_SUCH_KEY, null);
        }
    }

    public record ListAccess(Kind kind, Target forward, List<String> commonPrefixes) {
        public enum Kind { OK, NO_BUCKET, DENIED }

        static ListAccess forward(Target target) {
            return new ListAccess(Kind.OK, target, List.of());
        }

        static ListAccess virtual(List<String> prefixes) {
            return new ListAccess(Kind.OK, null, prefixes);
        }

        static ListAccess missingBucket() {
            return new ListAccess(Kind.NO_BUCKET, null, List.of());
        }

        static ListAccess denied() {
            return new ListAccess(Kind.DENIED, null, List.of());
        }
    }
}
