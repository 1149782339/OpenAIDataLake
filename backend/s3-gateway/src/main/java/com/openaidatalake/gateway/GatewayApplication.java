package com.openaidatalake.gateway;

import com.openaidatalake.domain.config.LakeProperties;
import com.openaidatalake.domain.crypto.AesTextCipher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.openaidatalake.domain")
@EnableJpaRepositories("com.openaidatalake.domain")
@EnableConfigurationProperties(LakeProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    AesTextCipher aesTextCipher(LakeProperties properties) {
        return AesTextCipher.fromBase64Key(properties.getMasterKey());
    }
}
