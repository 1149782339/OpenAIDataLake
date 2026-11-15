package com.openaidatalake.domain.path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathPrefixesTest {

    @Test
    void acceptsLogicalBucketWithUnderscore() {
        PathPrefixes.checkBucket("camera_capture_project");
        assertThrows(IllegalArgumentException.class, () -> PathPrefixes.checkBucket("Bad_Name"));
        assertThrows(IllegalArgumentException.class, () -> PathPrefixes.checkBucket("ab"));
    }

    @Test
    void normalizesDirectoryPrefix() {
        assertEquals("", PathPrefixes.normalizePrefix(null));
        assertEquals("", PathPrefixes.normalizePrefix("/"));
        assertEquals("bucket1/camera/", PathPrefixes.normalizePrefix("/bucket1/camera"));
        assertEquals("bucket1/camera/", PathPrefixes.normalizePrefix("bucket1/camera/"));
    }

    @Test
    void rejectsDotSegments() {
        assertThrows(IllegalArgumentException.class, () -> PathPrefixes.normalizePrefix("a/../b"));
        assertThrows(IllegalArgumentException.class, () -> PathPrefixes.normalizePrefix("a//b"));
    }

    @Test
    void coversOnDirectoryBoundary() {
        assertTrue(PathPrefixes.covers("bucket1/camera/", "bucket1/camera/a.jpg"));
        assertTrue(PathPrefixes.covers("bucket1/camera/", "bucket1/camera"));
        assertFalse(PathPrefixes.covers("bucket1/camera/", "bucket1/camera-2/a.jpg"));
        assertTrue(PathPrefixes.covers("", "bucket1/camera/a.jpg"));
        assertFalse(PathPrefixes.covers("bucket1/camera/", ""));
    }

    @Test
    void joinsPhysicalKey() {
        assertEquals("site-a/cam/a.jpg", PathPrefixes.join("site-a/cam/", "bucket1/camera/", "bucket1/camera/a.jpg"));
        assertEquals("site-a/cam/", PathPrefixes.join("site-a/cam/", "bucket1/camera/", "bucket1/camera/"));
        assertEquals("site-a/cam", PathPrefixes.join("site-a/cam/", "bucket1/camera/", "bucket1/camera"));
    }

    @Test
    void mapsPhysicalKeyBack() {
        assertEquals("bucket1/camera/a.jpg", PathPrefixes.toLogical("site-a/cam/", "bucket1/camera/", "site-a/cam/a.jpg"));
        assertEquals("bucket1/camera/", PathPrefixes.toLogical("site-a/cam/", "bucket1/camera/", "site-a/cam/"));
    }

    @Test
    void detectsOverlap() {
        assertTrue(PathPrefixes.overlaps("", "bucket1/camera/"));
        assertTrue(PathPrefixes.overlaps("bucket1/", "bucket1/camera/"));
        assertFalse(PathPrefixes.overlaps("bucket1/camera/", "bucket1/camera-2/"));
    }
}
