package com.codecopilot.repository;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.config.AppProperties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitCloneService {

    private final RepositoryStorageService storageService;
    private final AppProperties properties;

    public GitCloneService(RepositoryStorageService storageService, AppProperties properties) {
        this.storageService = storageService;
        this.properties = properties;
    }

    /**
     * Clones (or fetches into) the repository directory of the given GitRepository.
     * Uses the provided access token when present.
     */
    public void cloneOrFetch(GitRepository repository, String accessToken) {
        String url = repository.getUrl();
        if (url == null || url.isBlank()) {
            throw new BadRequestException("Repository URL is required");
        }
        File dir = storageService.ensureRepoDirectory(repository).toFile();
        String branch = repository.getBranch();
        try {
            if (dir.exists() && dir.listFiles() != null && dir.listFiles().length > 0) {
                try (Git git = Git.open(dir)) {
                    git.fetch().setForceUpdate(true).call();
                }
                return;
            }
            CloneCommand command = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir);
            if (branch != null && !branch.isBlank() && !"main".equals(branch)) {
                command.setBranch(branch);
            }
            if (accessToken != null && !accessToken.isBlank()) {
                command.setCredentialsProvider(new UsernamePasswordCredentialsProvider("x-access-token", accessToken));
            } else if (properties.getGithub().getToken() != null && !properties.getGithub().getToken().isBlank()) {
                command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        "x-access-token", properties.getGithub().getToken()));
            }
            command.call().close();
        } catch (Exception e) {
            throw new BadRequestException("Failed to clone repository: " + e.getMessage());
        }
    }

    public String resolveDefaultBranch(String url, String accessToken) {
        try {
            String token = accessToken != null && !accessToken.isBlank()
                    ? accessToken : properties.getGithub().getToken();
            org.eclipse.jgit.api.LsRemoteCommand cmd = Git.lsRemoteRepository()
                    .setRemote(url)
                    .setHeads(true);
            if (token != null && !token.isBlank()) {
                cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider("x-access-token", token));
            }
            String fallback = "main";
            for (var ref : cmd.call()) {
                if (ref.getName().startsWith("refs/heads/")) {
                    String name = ref.getName().substring("refs/heads/".length());
                    if (name.equals("main") || name.equals("master")) {
                        return name;
                    }
                    fallback = name;
                }
            }
            return fallback;
        } catch (Exception ignored) {
        }
        return "main";
    }
}