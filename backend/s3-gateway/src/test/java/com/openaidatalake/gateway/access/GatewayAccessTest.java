package com.openaidatalake.gateway.access;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayAccessTest {

    @Test
    void groupsRoutesIntoTheNextDirectory() {
        assertEquals(List.of("bucket1/"), GatewayAccess.group("", "/", List.of("bucket1/camera/", "bucket1/other/")));
        assertEquals(
                List.of("bucket1/camera/", "bucket1/other/"),
                GatewayAccess.group("bucket1/", "/", List.of("bucket1/camera/", "bucket1/other/")));
    }
}
