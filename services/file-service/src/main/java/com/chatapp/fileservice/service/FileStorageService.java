package com.chatapp.fileservice.service;

import com.chatapp.fileservice.model.FileMetadata;
import com.chatapp.fileservice.sns.FileSharedNotificationData;
import com.chatapp.fileservice.sns.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MetadataService metadataService;
    private final SnsPublisherService snsPublisherService;
    private final String bucketName;
    private final long presignExpirationMinutes;

    public FileStorageService(S3Client s3Client,
                              S3Presigner s3Presigner,
                              MetadataService metadataService,
                              SnsPublisherService snsPublisherService,
                              @Value("${file.bucket-name}") String bucketName,
                              @Value("${file.presign-expiration-minutes}") long presignExpirationMinutes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.metadataService = metadataService;
        this.snsPublisherService = snsPublisherService;
        this.bucketName = bucketName;
        this.presignExpirationMinutes = presignExpirationMinutes;
    }

    public FileMetadata upload(MultipartFile file,
                               String chatId,
                               String uploaderId,
                               String description,
                               java.util.Set<String> tags) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String fileId = UUID.randomUUID().toString();
        String originalName = safeFileName(file.getOriginalFilename());
        String s3Key = fileId + "/" + originalName;
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .metadata(Map.of("fileId", fileId))
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception ex) {
            throw new IllegalStateException("Failed to upload file to S3", ex);
        }

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setChatId(chatId);
        metadata.setUploaderId(uploaderId);
        metadata.setOriginalName(originalName);
        metadata.setContentType(contentType);
        metadata.setS3Bucket(bucketName);
        metadata.setS3Key(s3Key);
        metadata.setSize(file.getSize());
        metadata.setCreatedAt(Instant.now());
        metadata.setStatus(MetadataService.STATUS_ACTIVE);
        metadata.setDescription(description);
        metadata.setTags(tags);

        metadataService.save(metadata);

        FileSharedNotificationData data = new FileSharedNotificationData(
            uploaderId,
            chatId,
            fileId
        );
        snsPublisherService.publishEvent(NotificationType.FILE_SHARED, data);
        return metadata;
    }

    public ResponseInputStream<?> download(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(request);
    }

    public String createPresignedDownloadUrl(FileMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(metadata.getS3Bucket())
                .key(metadata.getS3Key())
                .build();
                
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();

        // Fix dla LocalStack: zamiana hosta kontenera na localhost dla przeglądarki
        return url.replace("localstack", "localhost");
    }


    private static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        return Paths.get(original).getFileName().toString();
    }
}
