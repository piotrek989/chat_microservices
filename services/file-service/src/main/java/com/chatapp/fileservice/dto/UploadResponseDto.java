package com.chatapp.fileservice.dto;

public class UploadResponseDto {
    private String fileId;
    private String presignedUrl;

    public UploadResponseDto() {
    }

    public UploadResponseDto(String fileId, String presignedUrl) {
        this.fileId = fileId;
        this.presignedUrl = presignedUrl;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }
}
