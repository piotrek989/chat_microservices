package com.chatapp.fileservice.service;

import com.chatapp.fileservice.dto.FileMetadataDto;
import com.chatapp.fileservice.dto.UploadResponseDto;
import com.chatapp.fileservice.model.FileMetadata;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileMetadataMapperTest {
    @Test
    void mapsMetadataAndPresignedUrlToDto() {
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-1");
        metadata.setChatId("chat-1");
        metadata.setUploaderId("user-1");
        metadata.setOriginalName("notes.txt");
        metadata.setContentType("text/plain");
        metadata.setS3Bucket("chat-files");
        metadata.setS3Key("uploads/file-1");
        metadata.setSize(42L);
        metadata.setCreatedAt(createdAt);
        metadata.setStatus("ACTIVE");
        metadata.setDescription("Meeting notes");
        metadata.setTags(Set.of("work", "notes"));

        FileMetadataDto result = FileMetadataMapper.toDtoWithPresignedUrl(metadata, "https://example.com/download");

        assertEquals(metadata.getFileId(), result.getFileId());
        assertEquals(metadata.getChatId(), result.getChatId());
        assertEquals(metadata.getUploaderId(), result.getUploaderId());
        assertEquals(metadata.getOriginalName(), result.getOriginalName());
        assertEquals(metadata.getContentType(), result.getContentType());
        assertEquals(metadata.getS3Bucket(), result.getS3Bucket());
        assertEquals(metadata.getS3Key(), result.getS3Key());
        assertEquals(metadata.getSize(), result.getSize());
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals(metadata.getStatus(), result.getStatus());
        assertEquals(metadata.getDescription(), result.getDescription());
        assertEquals(metadata.getTags(), result.getTags());
        assertEquals("https://example.com/download", result.getPresignedUrl());
    }

    @Test
    void uploadResponseContainsFileIdAndDownloadUrl() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-1");

        UploadResponseDto result = FileMetadataMapper.toUploadResponse(metadata, "https://example.com/download");

        assertEquals("file-1", result.getFileId());
        assertEquals("https://example.com/download", result.getPresignedUrl());
    }

    @Test
    void nullMetadataReturnsNullDtos() {
        assertNull(FileMetadataMapper.toDto(null));
        assertNull(FileMetadataMapper.toDtoWithPresignedUrl(null, "https://example.com/download"));
        assertNull(FileMetadataMapper.toUploadResponse(null, "https://example.com/download"));
    }
}
