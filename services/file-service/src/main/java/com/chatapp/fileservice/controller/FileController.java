package com.chatapp.fileservice.controller;

import com.chatapp.fileservice.dto.FileMetadataDto;
import com.chatapp.fileservice.dto.UploadResponseDto;
import com.chatapp.fileservice.model.FileMetadata;
import com.chatapp.fileservice.service.FileMetadataMapper;
import com.chatapp.fileservice.service.FileStorageService;
import com.chatapp.fileservice.service.MetadataService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {
    private final FileStorageService fileStorageService;
    private final MetadataService metadataService;

    public FileController(FileStorageService fileStorageService, MetadataService metadataService) {
        this.fileStorageService = fileStorageService;
        this.metadataService = metadataService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("chatId") String chatId,
                                                    @RequestParam("uploaderId") String uploaderId,
                                                    @RequestParam(value = "description", required = false) String description,
                                                    @RequestParam(value = "tags", required = false) Set<String> tags) {
        FileMetadata metadata = fileStorageService.upload(file, chatId, uploaderId, description, tags);
        String presignedUrl = fileStorageService.createPresignedDownloadUrl(metadata);
        return ResponseEntity.status(201).body(FileMetadataMapper.toUploadResponse(metadata, presignedUrl));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadataDto> getMetadata(@PathVariable String fileId) {
        Optional<FileMetadata> metadata = metadataService.findById(fileId);
        if (metadata.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String presignedUrl = fileStorageService.createPresignedDownloadUrl(metadata.get());
        return ResponseEntity.ok(FileMetadataMapper.toDtoWithPresignedUrl(metadata.get(), presignedUrl));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) {
        Optional<FileMetadata> metadataOpt = metadataService.findById(fileId);
        if (metadataOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FileMetadata metadata = metadataOpt.get();
        InputStreamResource resource = new InputStreamResource(
                fileStorageService.download(metadata.getS3Bucket(), metadata.getS3Key()));
        String contentType = metadata.getContentType() == null ? "application/octet-stream" : metadata.getContentType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataDto>> listByChatId(@RequestParam(value = "chatId", required = false) String chatId) {
        List<FileMetadataDto> results = metadataService.listByChatId(chatId)
                .stream()
                .map(item -> FileMetadataMapper.toDtoWithPresignedUrl(item, fileStorageService.createPresignedDownloadUrl(item)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}
