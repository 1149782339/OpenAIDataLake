package com.openaidatalake.domain.user;

import com.openaidatalake.domain.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_access_keys")
public class UserAccessKey extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_key", nullable = false, unique = true, length = 128)
    private String accessKey;

    @Column(name = "secret_key", nullable = false, length = 512)
    private String secretKey;

    @Column(nullable = false)
    private boolean enabled = true;

    protected UserAccessKey() {
    }

    public UserAccessKey(Long userId, String accessKey, String secretKey) {
        this.userId = userId;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
