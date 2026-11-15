package com.openaidatalake.console.service;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.user.UserAccount;
import com.openaidatalake.domain.user.UserAccountRepository;
import com.openaidatalake.domain.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserAdminService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}$");

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return users.findAll().stream().map(UserView::from).toList();
    }

    @Transactional
    public UserView create(String username, String password, UserRole role) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME.matcher(normalized).matches()) {
            throw ApiException.bad("用户名不合法");
        }
        if (password == null || password.length() < 8) {
            throw ApiException.bad("密码至少 8 位");
        }
        if (users.existsByUsername(normalized)) {
            throw ApiException.conflict("用户名已存在");
        }
        UserRole actual = role == null ? UserRole.USER : role;
        UserAccount saved = users.save(new UserAccount(normalized, passwordEncoder.encode(password), actual));
        return UserView.from(saved);
    }

    @Transactional
    public UserView update(long id, UserRole role, boolean enabled, String currentUsername) {
        UserAccount user = users.findById(id).orElseThrow(() -> ApiException.notFound("用户不存在"));
        if (user.getRole() == UserRole.ADMIN && (role != UserRole.ADMIN || !enabled)) {
            ensureAnotherAdmin(user.getId());
        }
        if (user.getUsername().equals(currentUsername) && !enabled) {
            throw ApiException.bad("不能停用当前登录用户");
        }
        user.setRole(role);
        user.setEnabled(enabled);
        return UserView.from(user);
    }

    @Transactional
    public void delete(long id, String currentUsername) {
        UserAccount user = users.findById(id).orElseThrow(() -> ApiException.notFound("用户不存在"));
        if (user.getUsername().equals(currentUsername)) {
            throw ApiException.bad("不能删除当前登录用户");
        }
        if (user.getRole() == UserRole.ADMIN) {
            ensureAnotherAdmin(user.getId());
        }
        users.delete(user);
    }

    private void ensureAnotherAdmin(long exceptId) {
        long admins = users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.ADMIN && user.isEnabled() && !user.getId().equals(exceptId))
                .count();
        if (admins == 0) {
            throw ApiException.bad("至少保留一个启用的管理员");
        }
    }

    public record UserView(long id, String username, UserRole role, boolean enabled, Instant createdAt) {
        static UserView from(UserAccount user) {
            return new UserView(user.getId(), user.getUsername(), user.getRole(), user.isEnabled(), user.getCreatedAt());
        }
    }
}
