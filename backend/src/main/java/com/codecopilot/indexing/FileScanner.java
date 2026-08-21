package com.codecopilot.indexing;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileScanner {

    public record IndexableFile(String relativePath, Path absolutePath, String language, boolean java) {
    }

    private final AppProperties properties;
    private final FileFilter fileFilter;
    private final LanguageDetector languageDetector;

    public FileScanner(AppProperties properties, FileFilter fileFilter, LanguageDetector languageDetector) {
        this.properties = properties;
        this.fileFilter = new FileFilter(readRootGitignore());
        this.languageDetector = languageDetector;
    }

    private List<String> readRootGitignore() {
        // Nothing repo-specific at construction time; per-repo patterns are added by indexer.
        return List.of();
    }

    public List<IndexableFile> scan(Path repositoryRoot) {
        return scan(repositoryRoot, fileFilter);
    }

    public List<IndexableFile> scan(Path repositoryRoot, FileFilter filter) {
        List<IndexableFile> files = new ArrayList<>();
        long maxBytes = properties.getIndexing().getMaxRepoSizeBytes();
        try (Stream<Path> stream = Files.walk(repositoryRoot)) {
            for (Path path : stream.sorted().toList()) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                String relative = repositoryRoot.relativize(path).toString().replace('\\', '/');
                if (filter.isIgnored(relative)) {
                    continue;
                }
                if (Files.size(path) > 5_000_000) {
                    continue; // skip oversized single files
                }
                String language = languageDetector.detect(path.getFileName().toString(), relative);
                if (language == null) {
                    continue;
                }
                files.add(new IndexableFile(relative, path, language, "java".equals(language)));
                if (files.size() > properties.getIndexing().getMaxFilesPerRepo()) {
                    throw new BadRequestException("Repository exceeds the max file count allowed for indexing");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to scan repository: " + e.getMessage());
        }
        return files;
    }

    /**
     * Reads the .gitignore (if present) and returns it so the filter can apply
     * its rules on top of the defaults.
     */
    public FileFilter withGitignore(Path repositoryRoot) {
        List<String> lines = new ArrayList<>();
        Path gi = repositoryRoot.resolve(".gitignore");
        if (Files.exists(gi)) {
            try {
                lines.addAll(Files.readAllLines(gi, java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        return new FileFilter(lines);
    }
}