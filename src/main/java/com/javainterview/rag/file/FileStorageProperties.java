package com.javainterview.rag.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "java-interview-rag.files")
public class FileStorageProperties {

    private String localRoot = ".tmp/file-objects";
    private S3 s3 = new S3();

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class S3 {
        private String endpoint;
        private String bucket = "java-interview-rag";
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private boolean autoCreateBucket = true;
        private Duration apiCallTimeout = Duration.ofSeconds(60);
        private Duration apiCallAttemptTimeout = Duration.ofSeconds(20);

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public boolean isAutoCreateBucket() { return autoCreateBucket; }
        public void setAutoCreateBucket(boolean autoCreateBucket) { this.autoCreateBucket = autoCreateBucket; }
        public Duration getApiCallTimeout() { return apiCallTimeout; }
        public void setApiCallTimeout(Duration apiCallTimeout) { this.apiCallTimeout = apiCallTimeout; }
        public Duration getApiCallAttemptTimeout() { return apiCallAttemptTimeout; }
        public void setApiCallAttemptTimeout(Duration apiCallAttemptTimeout) { this.apiCallAttemptTimeout = apiCallAttemptTimeout; }
    }
}
