package com.openaidatalake.console.datasource;

import com.openaidatalake.console.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceFieldsTest {

    @Test
    void acceptsSqlIdentifier() {
        assertEquals("postgres_datasource", DataSourceFields.name(" postgres_datasource "));
        assertEquals("localhost", DataSourceFields.host("localhost"));
        assertEquals("postgres", DataSourceFields.host("postgres"));
        assertEquals(5432, DataSourceFields.port(5432));
    }

    @Test
    void rejectsNameWithSlashOrLeadingDigit() {
        assertThrows(ApiException.class, () -> DataSourceFields.name("1abc"));
        assertThrows(ApiException.class, () -> DataSourceFields.name("my-source"));
        assertThrows(ApiException.class, () -> DataSourceFields.database("db/1"));
        assertThrows(ApiException.class, () -> DataSourceFields.password("bad\npass"));
    }
}
