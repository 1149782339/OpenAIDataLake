package com.openaidatalake.console;

import com.openaidatalake.domain.config.LakeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.openaidatalake.domain")
@EnableJpaRepositories("com.openaidatalake.domain")
@EnableConfigurationProperties(LakeProperties.class)
public class ConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }
}
