package com.codecopilot.repository;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.dto.ProjectDto;
import com.codecopilot.repository.dto.ConnectRequest;
import com.codecopilot.repository.dto.RepositoryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RepositoryService {

    private final GitRepositoryRepository repositoryRepository;
    private final RepositoryStorageService storageService;
    private final GitCloneService gitCloneService;
    private final ZipIngestService zipIngestService;
    private final ProjectAccessService accessService;

    public RepositoryService(GitRepositoryRepository repositoryRepository,
                             RepositoryStorageService storageService,
                             GitCloneService gitCloneService,
                             ZipIngestService zipIngestService,
                             ProjectAccessService accessService) {
        this.repositoryRepository = repositoryRepository;
        this.storageService = storageService;
        this.gitCloneService = gitCloneService;
        this.zipIngestService = zipIngestService;
        this.accessService = accessService;
    }

    @Transactional
    public RepositoryDto connect(Long projectId, ConnectRequest request, String accessToken) {
        accessService.requireEdit(projectId);
        String normalizedUrl = request.getUrl().trim();
        if (!(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://"))) {
            throw new BadRequestException("Repository URL must be http(s)");
        }
        GitRepository repository = new GitRepository();
        repository.setProjectId(projectId);
        repository.setUrl(normalizedUrl);
        String name = request.getName();
        if (name == null || name.isBlank()) {
            name = deriveName(normalizedUrl);
        }
        repository.setName(name);
        repository.setProvider(RepositoryProvider.GITHUB);
        repository.setBranch(request.getBranch() == null || request.getBranch().isBlank()
                ? gitCloneService.resolveDefaultBranch(normalizedUrl, accessToken)
                : request.getBranch().trim());
        repository = repositoryRepository.save(repository);
        try {
            gitCloneService.cloneOrFetch(repository, accessToken);
        } catch (RuntimeException e) {
            repositoryRepository.deleteById(repository.getId());
            throw e;
        }
        repository.setStatus("CLONED");
        return storageService.toDto(repositoryRepository.save(repository));
    }

    @Transactional
    public RepositoryDto upload(Long projectId, MultipartFile file, String name) {
        accessService.requireEdit(projectId);
        GitRepository repository = new GitRepository();
        repository.setProjectId(projectId);
        repository.setName(name == null || name.isBlank() ? deriveName(file.getOriginalFilename()) : name);
        repository.setProvider(RepositoryProvider.ZIP);
        repository.setBranch("N/A");
        repository.setUrl("zip://" + repository.getName());
        repository = repositoryRepository.save(repository);
        Path target = storageService.ensureRepoDirectory(repository);
        zipIngestService.extract(file, target);
        flattenSingleRootFolder(target);
        repository.setStatus("UPLOADED");
        return storageService.toDto(repositoryRepository.save(repository));
    }

    @Transactional(readOnly = true)
    public List<RepositoryDto> list(Long projectId) {
        accessService.requireView(projectId);
        return repositoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GitRepository requireRepository(Long repositoryId, Long projectId) {
        return repositoryRepository.findByIdAndProjectId(repositoryId, projectId)
                .orElseThrow(() -> new NotFoundException("Repository not found or not part of this project"));
    }

    @Transactional
    public void delete(Long repositoryId, Long projectId) {
        accessService.requireEdit(projectId);
        GitRepository repository = requireRepository(repositoryId, projectId);
        storageService.deleteRepositoryDirectory(repository);
        repositoryRepository.delete(repository);
    }

    @Transactional(readOnly = true)
    public long countByProject(Long projectId) {
        return repositoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId).size();
    }

    private void flattenSingleRootFolder(Path target) {
        try (Stream<Path> entries = Files.list(target)) {
            List<Path> children = entries.toList();
            if (children.size() == 1) {
                Path only = children.get(0);
                if (Files.isDirectory(only)) {
                    try (Stream<Path> walk = Files.walk(only)) {
                        List<Path> moving = walk.sorted(java.util.Comparator.reverseOrder()).toList();
                        for (Path p : moving) {
                            if (p.equals(only)) {
                                continue;
                            }
                            Path dest = target.resolve(only.getFileName().relativize(p));
                            Files.createDirectories(dest.getParent());
                            Files.move(p, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    try (Stream<Path> rest = Files.list(only)) {
                        rest.forEach(ignored -> {
                        });
                    }
                    Files.deleteIfExists(only);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String deriveName(String urlOrFile) {
        String base = urlOrFile == null ? "repository" : urlOrFile.trim();
        base = base.replace('\\', '/');
        if (base.contains("://")) {
            base = base.substring(base.indexOf("://") + 3);
        }
        if (base.endsWith(".git")) {
            base = base.substring(0, base.length() - 4);
        }
        String name = base.substring(base.lastIndexOf('/') + 1);
        return name.isBlank() ? "repository" : name;
    }
}