package com.openaidatalake.gateway.s3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigV4Test {

    @Test
    void matchesAwsGetObjectExample() {
        String canonical = SigV4.canonicalRequest(
                "GET",
                "/test.txt",
                "",
                List.of("host", "range", "x-amz-content-sha256", "x-amz-date"),
                Map.of(
                        "host", "examplebucket.s3.amazonaws.com",
                        "range", "bytes=0-9",
                        "x-amz-content-sha256", SigV4.EMPTY_SHA256,
                        "x-amz-date", "20130524T000000Z"),
                SigV4.EMPTY_SHA256);
        String signature = SigV4.signature(
                "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "20130524T000000Z",
                "20130524",
                "us-east-1",
                canonical);
        assertEquals("f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41", signature);
    }

    @Test
    void encodesPathWithoutEncodingSlashes() {
        assertEquals("a/b%20c", AwsUri.encodePath("a/b c"));
        assertEquals("/a/b%20c", AwsUri.encodeAbsolutePath("/a/b c"));
        assertEquals("a b", AwsUri.decodePercent("a%20b"));
    }

    @Test
    void sortsQueryForSigning() {
        QueryParameters query = QueryParameters.parse("prefix=b&list-type=2");
        assertEquals("list-type=2&prefix=b", query.canonical(java.util.Set.of()));
        assertTrue(query.has("list-type"));
    }
}
