package com.codecopilot.indexing;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeDependencyRepository;
import com.codecopilot.code.repo.CodeFieldRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.config.AppProperties;
import com.codecopilot.embedding.CodeChunkRepository;
import com.codecopilot.embedding.CodeEmbeddingRepository;
import com.codecopilot.embedding.CodeVectorStore;
import com.codecopilot.embedding.EmbeddingService;
import com.codecopilot.embedding.SemanticChunker;
import com.codecopilot.embedding.entity.CodeChunk;
import com.codecopilot.embedding.entity.CodeEmbedding;
import com.codecopilot.indexing.FileScanner.IndexableFile;
import com.codecopilot.parser.JavaSourceParser;
import com.codecopilot.parser.ParsedSourceFile;
import com.codecopilot.repository.GitRepository;
import com.codecopilot.repository.GitRepositoryRepository;
import com.codecopilot.repository.RepositoryStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final IndexJobRepository jobRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final RepositoryStorageService storageService;
    private final FileScanner fileScanner;
    private final JavaSourceParser javaSourceParser;
    private final com.codecopilot.code.CodeModelBuilder codeModelBuilder;
    private final RepositoryFileRepository fileRepository;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeFieldRepository fieldRepository;
    private final CodeDependencyRepository dependencyRepository;
    private final CodeReferenceRepository referenceRepository;
    private final CodeChunkRepository chunkRepository;
    private final CodeEmbeddingRepository embeddingRepository;
    private final CodeVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public IndexService(IndexJobRepository jobRepository, GitRepositoryRepository gitRepositoryRepository,
                        RepositoryStorageService storageService, FileScanner fileScanner,
                        JavaSourceParser javaSourceParser, com.codecopilot.code.CodeModelBuilder codeModelBuilder,
                        RepositoryFileRepository fileRepository, CodeClassRepository classRepository,
                        CodeMethodRepository methodRepository, CodeFieldRepository fieldRepository,
                        CodeDependencyRepository dependencyRepository, CodeReferenceRepository referenceRepository,
                        CodeChunkRepository chunkRepository, CodeEmbeddingRepository embeddingRepository,
                        CodeVectorStore vectorStore, EmbeddingService embeddingService,
                        AppProperties properties, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.storageService = storageService;
        this.fileScanner = fileScanner;
        this.javaSourceParser = javaSourceParser;
        this.codeModelBuilder = codeModelBuilder;
        this.fileRepository = fileRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.dependencyRepository = dependencyRepository;
        this.referenceRepository = referenceRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IndexJob createJob(Long repositoryId) {
        GitRepository gitRepo = gitRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        if (jobRepository.existsByRepositoryIdAndStatusIn(repositoryId,
                List.of(IndexJob.IndexStatus.PENDING, IndexJob.IndexStatus.RUNNING))) {
            throw new BadRequestException("An indexing job is already running for this repository");
        }
        IndexJob job = new IndexJob();
        job.setRepositoryId(repositoryId);
        job.setProjectId(gitRepo.getProjectId());
        job.setStatus(IndexJob.IndexStatus.PENDING);
        job.setIncremental(true);
        return jobRepository.save(job);
    }

    @Async("taskExecutor")
    public void indexAsync(Long jobId) {
        IndexJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Index job not found"));
        try {
            run(job);
        } catch (Exception e) {
            log.error("Indexing failed for job {}", jobId, e);
            job.setStatus(IndexJob.IndexStatus.FAILED);
            job.setError(e.getMessage() != null ? e.getMessage().substring(0, Math.min(1500, e.getMessage().length())) : e.toString());
            job.setFinishedAt(java.time.Instant.now());
            jobRepository.save(job);
        }
    }

    private void run(IndexJob job) throws Exception {
        job.setStatus(IndexJob.IndexStatus.RUNNING);
        job.setStartedAt(java.time.Instant.now());
        update(job);

        Long repoId = job.getRepositoryId();
        Long projectId = job.getProjectId();
        GitRepository gitRepo = gitRepositoryRepository.findById(repoId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        Path dir = storageService.repoDirectory(gitRepo);
        if (!Files.exists(dir)) {
            throw new BadRequestException("Repository has not been materialized on disk. Connect/upload first.");
        }

        job.setPhase("scanning");
        update(job);
        FileFilter filter = fileScanner.withGitignore(dir);
        List<IndexableFile> files = fileScanner.scan(dir, filter);

        Map<String, RepositoryFile> existing = new HashMap<>();
        for (RepositoryFile f : fileRepository.findByProjectIdAndRepositoryIdOrderByPath(projectId, repoId)) {
            existing.put(f.getPath(), f);
        }
        Set<String> scannedPaths = new HashSet<>();
        files.forEach(f -> scannedPaths.add(f.relativePath()));

        job.setPhase("removing deleted files");
        update(job);
        long removed = 0;
        for (Map.Entry<String, RepositoryFile> e : existing.entrySet()) {
            if (!scannedPaths.contains(e.getKey())) {
                deleteFileContent(e.getValue());
                removed++;
            }
        }
        job.setProgress(10);
        update(job);

        job.setPhase("parsing");
        update(job);
        long classCount = 0;
        long methodCount = 0;
        List<PendingChunk> pendingChunks = new ArrayList<>();
        int done = 0;
        SemanticChunker chunker = new SemanticChunker(
                properties.getIndexing().getChunk().getMaxChars(),
                properties.getIndexing().getChunk().getOverlap());

        for (IndexableFile file : files) {
            try {
                if (file.java()) {
                    ParsedSourceFile parsed = javaSourceParser.parse(file.absolutePath(), file.relativePath());
                    codeModelBuilder.persist(projectId, repoId, file.relativePath(), "java", file.absolutePath(), parsed);
                    classCount += parsed.types().size();
                    methodCount += parsed.types().stream().mapToLong(t -> t.methods().size()).sum();
                    RepositoryFile rf = fileRepository
                            .findByProjectIdAndRepositoryIdAndPath(projectId, repoId, file.relativePath())
                            .orElse(null);
                    if (rf != null) {
                        for (SemanticChunker.ChunkSpec spec : chunker.chunkJava(parsed)) {
                            pendingChunks.add(new PendingChunk(rf.getId(), file.relativePath(), spec));
                        }
                    }
                } else {
                    RepositoryFile rf = persistTextFile(projectId, repoId, file);
                    for (SemanticChunker.ChunkSpec spec : chunker.chunkText(file.relativePath(), readUtf8(file.absolutePath()))) {
                        pendingChunks.add(new PendingChunk(rf.getId(), file.relativePath(), spec));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to index {}: {}", file.relativePath(), e.getMessage());
            }
            done++;
            if (done % 100 == 0) {
                job.setProgress(Math.min(50, 10 + done * 40 / Math.max(1, files.size())));
                update(job);
            }
        }
        job.setProgress(50);
        update(job);

        job.setPhase("chunking");
        update(job);
        int newChunks = upsertChunks(projectId, repoId, pendingChunks);
        job.setProgress(70);
        update(job);

        job.setPhase("embedding");
        update(job);
        embedNewChunks(projectId, repoId);
        job.setProgress(92);
        update(job);

        gitRepo.setLastIndexedAt(java.time.Instant.now());
        gitRepo.setIndexedFileCount(files.size());
        gitRepositoryRepository.save(gitRepo);

        job.setFileCount(files.size());
        job.setClassCount(classCount);
        job.setMethodCount(methodCount);
        job.setChunkCount(chunkRepository.countByProjectIdAndRepositoryId(projectId, repoId));
        job.setStatus(IndexJob.IndexStatus.COMPLETED);
        job.setPhase("completed");
        job.setProgress(100);
        job.setFinishedAt(java.time.Instant.now());
        update(job);
        log.info("Indexed {} files ({} classes, {} methods) for repo {}. {} new chunks.",
                files.size(), classCount, methodCount, repoId, newChunks);
    }

    private void embedNewChunks(Long projectId, Long repositoryId) {
        List<CodeEmbedding> toSave = new ArrayList<>();
        List<CodeChunk> batchChunks = new ArrayList<>();
        List<String> batchTexts = new ArrayList<>();
        int batchSize = 64;

        Set<Long> alreadyEmbedded = new HashSet<>();
        for (CodeEmbedding e : embeddingRepository.findByProjectIdAndRepositoryId(projectId, repositoryId)) {
            alreadyEmbedded.add(e.getChunkId());
        }

        for (CodeChunk chunk : chunkRepository.findByProjectIdAndRepositoryId(projectId, repositoryId)) {
            if (alreadyEmbedded.contains(chunk.getId())) {
                continue;
            }
            batchChunks.add(chunk);
            batchTexts.add(chunk.getContent());
            if (batchChunks.size() >= batchSize) {
                toSave.addAll(embedBatch(repositoryId, batchChunks, batchTexts));
                batchChunks.clear();
                batchTexts.clear();
            }
        }
        if (!batchChunks.isEmpty()) {
            toSave.addAll(embedBatch(repositoryId, batchChunks, batchTexts));
        }
        if (!toSave.isEmpty()) {
            vectorStore.upsert(toSave);
        }
    }

    private List<CodeEmbedding> embedBatch(Long repositoryId, List<CodeChunk> chunks, List<String> texts) {
        List<float[]> vectors = embeddingService.embedAll(texts);
        List<CodeEmbedding> out = new ArrayList<>();
        for (int i = 0; i < vectors.size() && i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            CodeEmbedding emb = new CodeEmbedding();
            emb.setProjectId(chunk.getProjectId());
            emb.setRepositoryId(repositoryId);
            emb.setFileId(chunk.getFileId());
            emb.setChunkId(chunk.getId());
            try {
                emb.setVectorJson(objectMapper.writeValueAsString(toBoxed(vectors.get(i))));
            } catch (Exception e) {
                continue;
            }
            emb.setDimension(vectors.get(i).length);
            emb.setModel(embeddingService.modelName());
            out.add(emb);
        }
        return out;
    }

    private List<Double> toBoxed(float[] v) {
        List<Double> out = new ArrayList<>(v.length);
        for (float f : v) {
            out.add((double) f);
        }
        return out;
    }

    private int upsertChunks(Long projectId, Long repositoryId, List<PendingChunk> pending) {
        int created = 0;
        Map<Long, List<CodeChunk>> existingByFile = new HashMap<>();
        for (CodeChunk c : chunkRepository.findByProjectIdAndRepositoryId(projectId, repositoryId)) {
            existingByFile.computeIfAbsent(c.getFileId(), k -> new ArrayList<>()).add(c);
        }
        for (PendingChunk pc : pending) {
            String hash = sha1(pc.spec().content());
            boolean exists = existingByFile.getOrDefault(pc.fileId(), List.of()).stream()
                    .anyMatch(c -> hash.equals(c.getChecksum())
                            && c.getChunkType().equals(pc.spec().chunkType())
                            && c.getStartLine() == pc.spec().startLine());
            if (exists) {
                continue;
            }
            CodeChunk chunk = new CodeChunk();
            chunk.setProjectId(projectId);
            chunk.setRepositoryId(repositoryId);
            chunk.setFileId(pc.fileId());
            chunk.setChunkType(pc.spec().chunkType());
            chunk.setClassName(pc.spec().className());
            chunk.setMethodName(pc.spec().methodName());
            chunk.setFilePath(pc.path());
            chunk.setStartLine(pc.spec().startLine());
            chunk.setEndLine(pc.spec().endLine());
            chunk.setContent(pc.spec().content());
            chunk.setChecksum(hash);
            chunkRepository.save(chunk);
            created++;
        }
        return created;
    }

    private RepositoryFile persistTextFile(Long projectId, Long repositoryId, IndexableFile file) throws Exception {
        String content = readUtf8(file.absolutePath());
        String checksum = sha1(content);
        RepositoryFile existing = fileRepository
                .findByProjectIdAndRepositoryIdAndPath(projectId, repositoryId, file.relativePath())
                .orElse(null);
        if (existing != null && checksum.equals(existing.getChecksum())) {
            return existing;
        }
        RepositoryFile rf = existing != null ? existing : new RepositoryFile();
        rf.setProjectId(projectId);
        rf.setRepositoryId(repositoryId);
        rf.setPath(file.relativePath());
        rf.setFileName(file.relativePath().substring(file.relativePath().lastIndexOf('/') + 1));
        rf.setLanguage(file.language());
        rf.setSizeBytes(Files.size(file.absolutePath()));
        rf.setLineCount(countLines(content));
        rf.setChecksum(checksum);
        rf.setContent(content);
        return fileRepository.save(rf);
    }

    private void deleteFileContent(RepositoryFile file) {
        Long repoId = file.getRepositoryId();
        Long fileId = file.getId();
        classRepository.deleteByRepositoryIdAndFileId(repoId, fileId);
        methodRepository.deleteByRepositoryIdAndFileId(repoId, fileId);
        fieldRepository.deleteByRepositoryIdAndFileId(repoId, fileId);
        dependencyRepository.deleteByRepositoryIdAndFileId(repoId, fileId);
        referenceRepository.deleteByRepositoryIdAndFileId(repoId, fileId);
        chunkRepository.deleteByFileId(fileId);
        embeddingRepository.deleteByFileId(fileId);
        fileRepository.delete(file);
    }

    private void update(IndexJob job) {
        jobRepository.save(job);
    }

    private String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private int countLines(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n + 1;
    }

    private String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private record PendingChunk(Long fileId, String path, SemanticChunker.ChunkSpec spec) {
    }
}