package com.codecopilot.repository;

import com.codecopilot.config.AppProperties;
import com.codecopilot.repository.dto.RepositoryDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
public class RepositoryStorageService {

    private final Path root;

    public RepositoryStorageService(AppProperties properties) {
        this.root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path repoDirectory(GitRepository repository) {
        return root.resolve(String.valueOf(repository.getProjectId()))
                .resolve(repository.getId().toString()).normalize();
    }

    public Path ensureRepoDirectory(GitRepository repository) {
        Path dir = repoDirectory(repository);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create repository directory", e);
        }
        return dir;
    }

    public boolean repoDirectoryExists(GitRepository repository) {
        return Files.exists(repoDirectory(repository));
    }

    /**
     * Guards against path traversal: the target resolved from a relative path must
     * stay inside the storage root.
     */
    public Path resolveInside(Path base, String relativePath) {
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw new SecurityException("Path traversal blocked: " + relativePath);
        }
        return target;
    }

    public void deleteRepositoryDirectory(GitRepository repository) {
        Path dir = repoDirectory(repository);
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete repository directory", e);
            }
        }
    }

    public RepositoryDto toDto(GitRepository repo) {
        return RepositoryDto.builder()
                .id(repo.getId())
                .projectId(repo.getProjectId())
                .name(repo.getName())
                .url(repo.getUrl())
                .branch(repo.getBranch())
                .provider(repo.getProvider().name())
                .status(repo.getStatus())
                .indexedFileCount(repo.getIndexedFileCount())
                .lastIndexedAt(repo.getLastIndexedAt())
                .createdAt(repo.getCreatedAt())
                .build();
    }
}