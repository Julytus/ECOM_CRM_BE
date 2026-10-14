package vn.aeoc.crm.controller.other;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.service.other.FileStorageService;
import vn.aeoc.crm.util.Mapper;

@RestController
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/file/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        var result = fileStorageService.upload(file);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
