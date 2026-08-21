package com.codecopilot.repository;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipIngestService {

    private static final long MAX_ZIP_SIZE = 250_000_000L;
    private static final int MAX_ENTRIES = 100_000;

    private final AppProperties properties;

    public ZipIngestService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Extracts a repository ZIP safely: validates it is a real zip, enforces
     * size/entry limits, and blocks path traversal.
     */
    public void extract(MultipartFile file, Path targetDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("ZIP file is required");
        }
        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new BadRequestException("ZIP file exceeds the 250MB limit");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".zip")) {
            throw new BadRequestException("Only .zip archives are supported");
        }
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new BadRequestException("Cannot create extraction directory");
        }

        long totalSize = 0;
        int count = 0;
        try (InputStream raw = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                count++;
                if (count > MAX_ENTRIES) {
                    throw new BadRequestException("ZIP contains too many entries (limit 100000)");
                }
                totalSize += Math.max(entry.getSize(), 0);
                if (totalSize > MAX_ZIP_SIZE) {
                    throw new BadRequestException("ZIP contents exceed the 250MB limit");
                }
                // Path traversal protection
                Path target = targetDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetDir)) {
                    throw new BadRequestException("ZIP contains an unsafe path: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                byte[] buffer = new byte[8192];
                try (var out = Files.newOutputStream(target)) {
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Invalid ZIP archive: " + e.getMessage());
        }
    }
}