package com.chatapp.fileservice.service;

import com.chatapp.fileservice.dto.FileMetadataDto;
import com.chatapp.fileservice.dto.UploadResponseDto;
import com.chatapp.fileservice.model.FileMetadata;

public class FileMetadataMapper {
    private FileMetadataMapper() {
    }

    public static FileMetadataDto toDto(FileMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        FileMetadataDto dto = new FileMetadataDto();
        dto.setFileId(metadata.getFileId());
        dto.setChatId(metadata.getChatId());
        dto.setUploaderId(metadata.getUploaderId());
        dto.setOriginalName(metadata.getOriginalName());
        dto.setContentType(metadata.getContentType());
        dto.setS3Bucket(metadata.getS3Bucket());
        dto.setS3Key(metadata.getS3Key());
        dto.setSize(metadata.getSize());
        dto.setCreatedAt(metadata.getCreatedAt());
        dto.setStatus(metadata.getStatus());
        dto.setDescription(metadata.getDescription());
        dto.setTags(metadata.getTags());
        return dto;
    }

    public static FileMetadataDto toDtoWithPresignedUrl(FileMetadata metadata, String presignedUrl) {
        FileMetadataDto dto = toDto(metadata);
        if (dto != null) {
            dto.setPresignedUrl(presignedUrl);
        }
        return dto;
    }

    public static UploadResponseDto toUploadResponse(FileMetadata metadata, String presignedUrl) {
        if (metadata == null) {
            return null;
        }
        return new UploadResponseDto(metadata.getFileId(), presignedUrl);
    }
}
