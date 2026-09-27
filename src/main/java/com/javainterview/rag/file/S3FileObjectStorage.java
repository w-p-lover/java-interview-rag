package com.javainterview.rag.file;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Objects;

public final class S3FileObjectStorage implements FileObjectStorage {

    private final S3Client client;
    private final String bucket;

    public S3FileObjectStorage(S3Client client, String bucket, boolean autoCreateBucket) {
        this.client = Objects.requireNonNull(client, "S3 客户端不能为空");
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 Bucket 不能为空");
        }
        this.bucket = bucket.strip();
        ensureBucket(autoCreateBucket);
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey)
                    .contentType(contentType).contentLength((long) content.length).build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            throw unavailable("S3 文件写入失败", exception);
        }
    }

    @Override
    public byte[] read(String objectKey) {
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                    .asByteArray();
        } catch (NoSuchKeyException exception) {
            throw new FileAssetUnavailableException("文件对象不存在");
        } catch (S3Exception exception) {
            throw unavailable("S3 文件读取失败", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw unavailable("S3 文件检查失败", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (S3Exception exception) {
            throw unavailable("S3 文件删除失败", exception);
        }
    }

    private void ensureBucket(boolean autoCreateBucket) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404 || !autoCreateBucket) {
                throw unavailable("S3 Bucket 检查失败", exception);
            }
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception createException) {
                if (createException.statusCode() != 409) {
                    throw unavailable("S3 Bucket 创建失败", createException);
                }
            }
        }
    }

    private FileAssetUnavailableException unavailable(String message, S3Exception exception) {
        return new FileAssetUnavailableException(message + "，状态码 " + exception.statusCode(), exception);
    }
}
