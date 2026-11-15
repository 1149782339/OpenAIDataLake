package com.openaidatalake.console.bootstrap;

import com.openaidatalake.domain.config.LakeProperties;
import com.openaidatalake.domain.user.UserAccount;
import com.openaidatalake.domain.user.UserAccountRepository;
import com.openaidatalake.domain.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LakeProperties properties;

    public AdminSeeder(UserAccountRepository users, PasswordEncoder passwordEncoder, LakeProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String username = properties.getAdmin().getUsername() == null ? "" : properties.getAdmin().getUsername().trim();
        String password = properties.getAdmin().getPassword() == null ? "" : properties.getAdmin().getPassword();
        if (username.isBlank() || password.isBlank()) {
            log.warn("用户表是空的，且没有配置 LAKE_ADMIN_USERNAME / LAKE_ADMIN_PASSWORD，控制台暂时无法登录");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("LAKE_ADMIN_PASSWORD 至少 8 位");
        }
        users.save(new UserAccount(username, passwordEncoder.encode(password), UserRole.ADMIN));
        log.info("已创建初始管理员 {}", username);
    }
}
