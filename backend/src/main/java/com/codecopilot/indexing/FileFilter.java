package com.codecopilot.indexing;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Configurable repository file filtering. Applies a default ignore set plus
 * basic .gitignore patterns per repository.
 */
@Component
public class FileFilter {

    public static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "node_modules", "target", "build", "dist", "out", ".git", ".idea", ".vscode",
            ".gradle", ".settings", "__pycache__", ".venv", "venv", "coverage", ".next",
            ".nuxt", "logs", "tmp", "temp", "bin", "obj");

    public static final Set<String> IGNORED_EXTENSIONS = Set.of(
            "class", "jar", "war", "ear", "exe", "dll", "so", "dylib", "png", "jpg", "jpeg",
            "gif", "bmp", "ico", "svg", "mp4", "mp3", "wav", "zip", "gz", "tgz", "rar", "7z",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "woff", "woff2", "ttf", "eot",
            "o", "a", "obj", "pyc", "pyo", "lock");

    public static final Set<String> IGNORED_FILES = Set.of(
            ".DS_Store", "Thumbs.db", ".gitignore", ".gitattributes", "package-lock.json", "yarn.lock", ".env");

    private final java.util.regex.Pattern pattern;

    public FileFilter() {
        this.pattern = null;
    }

    public FileFilter(List<String> gitignoreLines) {
        List<String> regexes = new java.util.ArrayList<>();
        for (String raw : gitignoreLines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("!") || line.startsWith("/")) {
                line = line.substring(1);
            }
            if (line.endsWith("/")) {
                line = line.substring(0, line.length() - 1);
            }
            String regex = java.util.regex.Pattern.quote(line)
                    .replaceAll("\\\\\\*\\*", ".*")
                    .replaceAll("\\\\\\*", "[^/]*")
                    .replaceAll("\\\\\\?", ".");
            regexes.add("(^|/)" + regex + "($|/)");
        }
        this.pattern = regexes.isEmpty() ? null
                : java.util.regex.Pattern.compile(String.join("|", regexes));
    }

    public boolean isIgnored(String relativePath) {
        String path = relativePath.replace('\\', '/');
        String[] parts = path.split("/");
        for (String part : parts) {
            if (IGNORED_DIRECTORIES.contains(part)) {
                return true;
            }
        }
        String fileName = parts[parts.length - 1];
        if (IGNORED_FILES.contains(fileName)) {
            return true;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && IGNORED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase())) {
            return true;
        }
        return pattern != null && pattern.matcher(path).find();
    }
}