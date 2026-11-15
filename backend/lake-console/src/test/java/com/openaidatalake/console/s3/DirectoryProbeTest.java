package com.openaidatalake.console.s3;

import com.openaidatalake.domain.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DirectoryProbeTest {

    @Test
    void signatureMatchesAwsGetExample() {
        String canonical = S3Signer.canonicalRequest(
                "GET",
                "/test.txt",
                "",
                List.of("host", "range", "x-amz-content-sha256", "x-amz-date"),
                Map.of(
                        "host", "examplebucket.s3.amazonaws.com",
                        "range", "bytes=0-9",
                        "x-amz-content-sha256", S3Signer.EMPTY_SHA256,
                        "x-amz-date", "20130524T000000Z"),
                S3Signer.EMPTY_SHA256);
        String signature = S3Signer.signature(
                "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "20130524T000000Z",
                "20130524",
                "us-east-1",
                canonical);
        assertEquals("f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41", signature);
    }

    @Test
    void infersProviderFromEndpoint() {
        StorageProfile minio = StorageProfile.infer(URI.create("http://10.0.0.8:9000"));
        assertEquals(StorageProvider.MINIO, minio.provider());
        assertEquals("us-east-1", minio.region());
        assertEquals(true, minio.pathStyle());

        StorageProfile oss = StorageProfile.infer(URI.create("https://oss-cn-hangzhou.aliyuncs.com"));
        assertEquals(StorageProvider.OSS, oss.provider());
        assertEquals("cn-hangzhou", oss.region());
        assertFalse(oss.pathStyle());

        StorageProfile aws = StorageProfile.infer(URI.create("https://s3.ap-southeast-1.amazonaws.com"));
        assertEquals(StorageProvider.AWS, aws.provider());
        assertEquals("ap-southeast-1", aws.region());

        StorageProfile awsGlobal = StorageProfile.infer(URI.create("https://s3.amazonaws.com"));
        assertEquals("us-east-1", awsGlobal.region());
    }

    @Test
    void readsBucketsAndCommonPrefixes() {
        byte[] buckets = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Buckets>
                    <Bucket><Name>captures</Name></Bucket>
                    <Bucket><Name>raw</Name></Bucket>
                  </Buckets>
                </ListAllMyBucketsResult>
                """.getBytes(StandardCharsets.UTF_8);
        assertEquals(List.of("captures", "raw"), S3ListXml.bucketNames(buckets));

        byte[] listing = """
                <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Prefix>site-a/</Prefix>
                  <IsTruncated>true</IsTruncated>
                  <NextContinuationToken>site-a%2Fcam</NextContinuationToken>
                  <CommonPrefixes><Prefix>site-a%2Fcam/</Prefix></CommonPrefixes>
                </ListBucketResult>
                """.getBytes(StandardCharsets.UTF_8);
        S3ListXml.PrefixPage page = S3ListXml.prefixes(listing);
        assertEquals(List.of("site-a/cam/"), page.prefixes());
        assertEquals("site-a/cam", page.nextToken());
    }

    @Test
    void readsErrorMessage() {
        byte[] body = """
                <Error><Code>InvalidAccessKeyId</Code><Message>The access key is invalid.</Message></Error>
                """.getBytes(StandardCharsets.UTF_8);
        assertEquals("The access key is invalid.", S3ListXml.errorMessage(body));
        assertNull(S3ListXml.errorMessage("not xml".getBytes(StandardCharsets.UTF_8)));
    }
}
