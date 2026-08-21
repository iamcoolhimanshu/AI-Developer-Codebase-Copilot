package com.codecopilot.indexing;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class LanguageDetector {

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("java", "java"),
            Map.entry("js", "javascript"),
            Map.entry("mjs", "javascript"),
            Map.entry("jsx", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("tsx", "typescript"),
            Map.entry("py", "python"),
            Map.entry("go", "go"),
            Map.entry("rs", "rust"),
            Map.entry("c", "c"),
            Map.entry("h", "c"),
            Map.entry("cpp", "cpp"),
            Map.entry("hpp", "cpp"),
            Map.entry("cs", "csharp"),
            Map.entry("rb", "ruby"),
            Map.entry("php", "php"),
            Map.entry("kt", "kotlin"),
            Map.entry("kts", "kotlin"),
            Map.entry("swift", "swift"),
            Map.entry("sql", "sql"),
            Map.entry("html", "html"),
            Map.entry("htm", "html"),
            Map.entry("css", "css"),
            Map.entry("scss", "scss"),
            Map.entry("less", "less"),
            Map.entry("vue", "vue"),
            Map.entry("json", "json"),
            Map.entry("xml", "xml"),
            Map.entry("yml", "yaml"),
            Map.entry("yaml", "yaml"),
            Map.entry("properties", "properties"),
            Map.entry("toml", "toml"),
            Map.entry("ini", "ini"),
            Map.entry("md", "markdown"),
            Map.entry("markdown", "markdown"),
            Map.entry("txt", "text"),
            Map.entry("sh", "shell"),
            Map.entry("bash", "shell"),
            Map.entry("bat", "batch"),
            Map.entry("cmd", "batch"),
            Map.entry("ps1", "powershell"),
            Map.entry("gradle", "groovy"),
            Map.entry("groovy", "groovy"),
            Map.entry("tf", "hcl"),
            Map.entry("dockerfile", "dockerfile"),
            Map.entry("proto", "protobuf")
    );

    private static final Set<String> KNOWN_FILENAMES = Set.of(
            "dockerfile", "makefile", "pom.xml", "build.gradle", "build.gradle.kts",
            "settings.gradle", "settings.gradle.kts", "application.yml", "application.yaml",
            "application.properties", "package.json", ".npmrc", "nginx.conf");

    public String detect(String fileName, String relativePath) {
        if (relativePath != null) {
            String lower = relativePath.replace('\\', '/').toLowerCase();
            String fname = lower.substring(lower.lastIndexOf('/') + 1);
            if (fname.equals("dockerfile")) {
                return "dockerfile";
            }
            if (fname.equals("makefile")) {
                return "makefile";
            }
            if (fname.equals(".env")) {
                return "dotenv";
            }
            if (fname.contains("application") && (fname.endsWith(".yml") || fname.endsWith(".yaml"))) {
                return "yaml-config";
            }
            if (fname.contains("application") && fname.endsWith(".properties")) {
                return "properties";
            }
        }
        String name = fileName == null ? "" : fileName;
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase();
        return EXTENSIONS.get(ext);
    }
}