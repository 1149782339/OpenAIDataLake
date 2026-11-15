package com.openaidatalake.gateway.s3;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ListXmlTest {

    @Test
    void rewritesPhysicalKeysToLogicalKeys() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Name>captures</Name>
                  <Prefix>site-a/cam/</Prefix>
                  <KeyCount>1</KeyCount>
                  <MaxKeys>1000</MaxKeys>
                  <EncodingType>url</EncodingType>
                  <IsTruncated>false</IsTruncated>
                  <Contents>
                    <Key>site-a/cam/a%20b.jpg</Key>
                    <Size>1</Size>
                  </Contents>
                  <CommonPrefixes>
                    <Prefix>site-a/cam/sub/</Prefix>
                  </CommonPrefixes>
                </ListBucketResult>
                """;
        byte[] rewritten = ListXml.rewrite(
                xml.getBytes(StandardCharsets.UTF_8),
                "camera_capture_project",
                "bucket1/camera/",
                "site-a/cam/",
                "bucket1/camera/",
                false);
        String text = new String(rewritten, StandardCharsets.UTF_8);
        assertTrue(text.contains("camera_capture_project"));
        assertTrue(text.contains("bucket1/camera/a b.jpg"));
        assertTrue(text.contains("bucket1/camera/sub/"));
        assertTrue(!text.contains("site-a/cam/"));
    }
}
