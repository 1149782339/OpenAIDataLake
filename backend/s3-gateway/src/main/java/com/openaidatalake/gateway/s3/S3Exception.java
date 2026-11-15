package com.openaidatalake.gateway.s3;

public class S3Exception extends RuntimeException {

    private final int status;
    private final String code;

    public S3Exception(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
