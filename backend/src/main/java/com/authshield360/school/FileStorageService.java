package com.authshield360.school;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Stores/loads uploaded assignment files with type and size validation (UC-A1/A4). */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "rtf", "odt",
            "ppt", "pptx", "xls", "xlsx",
            "zip", "rar", "7z",
            "png", "jpg", "jpeg", "gif", "webp");

    private final AppProperties props;
    private final Path root;

    public FileStorageService(AppProperties props) {
        this.props = props;
        this.root = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
    }

    public record StoredFile(String storedName, String originalName, String contentType, long size) { }

    public StoredFile store(MultipartFile file, Long assignmentId, Long studentId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        if (file.getSize() > props.getMaxUploadBytes()) {
            throw new BusinessException(ErrorCode.INVALID_FILE,
                    "Tệp vượt quá dung lượng cho phép (" + (props.getMaxUploadBytes() / (1024 * 1024)) + " MB).");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extensionOf(original);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE,
                    "Định dạng tệp không được hỗ trợ: ." + extension);
        }
        String storedName = "assignments/" + assignmentId + "/" + UUID.randomUUID() + "-s" + studentId + "." + extension;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Không thể lưu tệp. Vui lòng thử lại.");
        }
        return new StoredFile(storedName, original, file.getContentType(), file.getSize());
    }

    public Resource load(String storedName) {
        if (storedName == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        try {
            Path target = root.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Tệp không tồn tại trên hệ thống.");
            }
            return resource;
        } catch (java.net.MalformedURLException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        String cleaned = name.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) cleaned = cleaned.substring(slash + 1);
        return cleaned.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
