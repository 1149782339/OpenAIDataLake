package com.openaidatalake.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccessKeyRepository extends JpaRepository<UserAccessKey, Long> {

    Optional<UserAccessKey> findByAccessKey(String accessKey);

    List<UserAccessKey> findByUserIdOrderByIdDesc(Long userId);

    List<UserAccessKey> findAllByOrderByIdDesc();
}
