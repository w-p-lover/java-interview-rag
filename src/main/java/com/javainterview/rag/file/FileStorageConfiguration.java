package com.javainterview.rag.file;

import com.javainterview.rag.file.persistence.FileAssetMapper;
import com.javainterview.rag.file.persistence.MySqlFileAssetStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfiguration {

    @Bean
    @Profile("!storage")
    FileAssetStore inMemoryFileAssetStore() {
        return new InMemoryFileAssetStore();
    }

    @Bean
    @Profile("storage")
    FileAssetStore mySqlFileAssetStore(FileAssetMapper mapper) {
        return new MySqlFileAssetStore(mapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "java-interview-rag.files", name = "backend",
            havingValue = "local", matchIfMissing = true)
    FileObjectStorage localFileObjectStorage(FileStorageProperties properties) {
        return new LocalFileObjectStorage(Path.of(properties.getLocalRoot()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "java-interview-rag.files", name = "backend", havingValue = "s3")
    S3Client fileS3Client(FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        requireS3(s3.getEndpoint(), "S3 endpoint");
        requireS3(s3.getAccessKey(), "S3 access key");
        requireS3(s3.getSecretKey(), "S3 secret key");
        ClientOverrideConfiguration override = ClientOverrideConfiguration.builder()
                .apiCallTimeout(s3.getApiCallTimeout())
                .apiCallAttemptTimeout(s3.getApiCallAttemptTimeout())
                .build();
        return S3Client.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                .overrideConfiguration(override)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "java-interview-rag.files", name = "backend", havingValue = "s3")
    FileObjectStorage s3FileObjectStorage(S3Client fileS3Client, FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        return new S3FileObjectStorage(fileS3Client, s3.getBucket(), s3.isAutoCreateBucket());
    }

    @Bean
    FileAssetService fileAssetService(FileAssetStore store, FileObjectStorage objectStorage) {
        return new FileAssetService(store, objectStorage);
    }

    private void requireS3(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
    }
}
