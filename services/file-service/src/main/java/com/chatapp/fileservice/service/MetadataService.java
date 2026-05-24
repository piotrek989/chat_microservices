package com.chatapp.fileservice.service;

import com.chatapp.fileservice.model.FileMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class MetadataService {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public MetadataService(DynamoDbClient dynamoDbClient,
                           @Value("${file.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void save(FileMetadata metadata) {
        if (metadata.getCreatedAt() == null) {
            metadata.setCreatedAt(Instant.now());
        }
        if (metadata.getStatus() == null) {
            metadata.setStatus(STATUS_ACTIVE);
        }
        Map<String, AttributeValue> item = toItem(metadata);
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    public Optional<FileMetadata> findById(String fileId) {
        Map<String, AttributeValue> key = Map.of("fileId", AttributeValue.builder().s(fileId).build());
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()).item();
        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromItem(item));
    }

    public List<FileMetadata> listByChatId(String chatId) {
        if (chatId == null) {
            return scanAll();
        }
        String normalized = chatId.trim();
        if (normalized.isEmpty()) {
            return scanAll();
        }
        List<FileMetadata> all = scanAll();
        List<FileMetadata> filtered = new ArrayList<>();
        for (FileMetadata item : all) {
            if (normalized.equals(item.getChatId())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private List<FileMetadata> scanAll() {
        return scan(ScanRequest.builder().tableName(tableName).build());
    }

    private List<FileMetadata> scan(ScanRequest request) {
        ScanResponse response = dynamoDbClient.scan(request);
        List<FileMetadata> results = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            results.add(fromItem(item));
        }
        return results;
    }

    private static Map<String, AttributeValue> toItem(FileMetadata metadata) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("fileId", AttributeValue.builder().s(metadata.getFileId()).build());
        putString(item, "chatId", metadata.getChatId());
        putString(item, "uploaderId", metadata.getUploaderId());
        putString(item, "originalName", metadata.getOriginalName());
        putString(item, "contentType", metadata.getContentType());
        putString(item, "s3Bucket", metadata.getS3Bucket());
        putString(item, "s3Key", metadata.getS3Key());
        if (metadata.getSize() != null) {
            item.put("size", AttributeValue.builder().n(String.valueOf(metadata.getSize())).build());
        }
        if (metadata.getCreatedAt() != null) {
            item.put("createdAt", AttributeValue.builder().s(metadata.getCreatedAt().toString()).build());
        }
        putString(item, "status", metadata.getStatus());
        putString(item, "description", metadata.getDescription());
        Set<String> tags = metadata.getTags();
        if (tags != null && !tags.isEmpty()) {
            item.put("tags", AttributeValue.builder().ss(tags).build());
        }
        return item;
    }

    private static void putString(Map<String, AttributeValue> item, String name, String value) {
        if (value != null) {
            item.put(name, AttributeValue.builder().s(value).build());
        }
    }

    private static FileMetadata fromItem(Map<String, AttributeValue> item) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(getString(item, "fileId"));
        metadata.setChatId(getString(item, "chatId"));
        metadata.setUploaderId(getString(item, "uploaderId"));
        metadata.setOriginalName(getString(item, "originalName"));
        metadata.setContentType(getString(item, "contentType"));
        metadata.setS3Bucket(getString(item, "s3Bucket"));
        metadata.setS3Key(getString(item, "s3Key"));
        metadata.setStatus(getString(item, "status"));
        metadata.setDescription(getString(item, "description"));
        AttributeValue size = item.get("size");
        if (size != null && size.n() != null) {
            metadata.setSize(Long.parseLong(size.n()));
        }
        AttributeValue createdAt = item.get("createdAt");
        if (createdAt != null && createdAt.s() != null) {
            metadata.setCreatedAt(Instant.parse(createdAt.s()));
        }
        AttributeValue tags = item.get("tags");
        if (tags != null && tags.ss() != null && !tags.ss().isEmpty()) {
            metadata.setTags(Set.copyOf(tags.ss()));
        }
        return metadata;
    }

    private static String getString(Map<String, AttributeValue> item, String name) {
        AttributeValue value = item.get(name);
        return value == null ? null : value.s();
    }
}
