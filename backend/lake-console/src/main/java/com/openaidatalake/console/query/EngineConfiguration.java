package com.openaidatalake.console.query;

import com.openaidatalake.console.service.DuckDbConnectionPool;
import com.openaidatalake.engine.DuckDbEngine;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
class EngineConfiguration {

    /** DuckDbEngine 也是 DataSource。控制台库标成主数据源，Spring Boot 才不会拿 DuckDB 替换 Postgres。 */
    @Bean
    @Primary
    DataSource applicationDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(destroyMethod = "close")
    DuckDbEngine duckDbEngine() {
        return new DuckDbEngine();
    }

    @Bean(destroyMethod = "close")
    DuckDbConnectionPool duckDbConnectionPool(DuckDbEngine engine) {
        return new DuckDbConnectionPool(engine);
    }
}
