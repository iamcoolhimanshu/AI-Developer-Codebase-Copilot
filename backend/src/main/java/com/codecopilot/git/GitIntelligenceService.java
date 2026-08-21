package com.codecopilot.git;

import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.repository.GitRepository;
import com.codecopilot.repository.GitRepositoryRepository;
import com.codecopilot.repository.RepositoryStorageService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitIntelligenceService {

    private final GitRepositoryRepository repositoryRepository;
    private final RepositoryStorageService storageService;
    private final ProjectAccessService accessService;

    public GitIntelligenceService(GitRepositoryRepository repositoryRepository,
                                  RepositoryStorageService storageService, ProjectAccessService accessService) {
        this.repositoryRepository = repositoryRepository;
        this.storageService = storageService;
        this.accessService = accessService;
    }

    public record CommitInfo(String id, String shortId, String message, String author, String email,
                             long time, int changeCount) {
    }

    public record FileChange(String changeType, String oldPath, String newPath, int linesAdded, int linesDeleted) {
    }

    public record DiffResult(String commitId, List<FileChange> changes, String unifiedDiff) {
    }

    public record BlameLine(int line, String commitId, String author, long time) {
    }

    public List<CommitInfo> commits(Long projectId, Long repoId, String filePath, int max) {
        accessService.requireView(projectId);
        GitRepository repo = requireRepo(repoId, projectId);
        try (Git git = Git.open(storageService.repoDirectory(repo).toFile())) {
            List<CommitInfo> out = new ArrayList<>();
            var log = git.log().setMaxCount(Math.min(200, max <= 0 ? 50 : max));
            if (filePath != null && !filePath.isBlank()) {
                log.addPath(filePath);
            }
            for (RevCommit commit : log.call()) {
                int changes = changedFiles(git, commit).size();
                PersonIdent author = commit.getAuthorIdent();
                out.add(new CommitInfo(commit.getName(), commit.getName().substring(0, Math.min(8, commit.getName().length())),
                        commit.getShortMessage(), author == null ? "unknown" : author.getName(),
                        author == null ? "" : author.getEmailAddress(), commit.getCommitTime() * 1000L, changes));
            }
            return out;
        } catch (Exception e) {
            throw new NotFoundException("Git history unavailable: " + e.getMessage());
        }
    }

    public DiffResult diff(Long projectId, Long repoId, String commitId) {
        accessService.requireView(projectId);
        GitRepository repo = requireRepo(repoId, projectId);
        try (Git git = Git.open(storageService.repoDirectory(repo).toFile());
             RevWalk walk = new RevWalk(git.getRepository())) {
            ObjectId id = git.getRepository().resolve(commitId);
            RevCommit commit = walk.parseCommit(id);
            RevCommit parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0).getId()) : null;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DiffFormatter fmt = new DiffFormatter(out)) {
                fmt.setRepository(git.getRepository());
                fmt.setDiffComparator(RawTextComparator.DEFAULT);
                List<DiffEntry> entries;
                if (parent == null) {
                    RevTree tree = walk.parseTree(commit.getTree());
                    entries = fmt.scan(null, tree);
                } else {
                    entries = fmt.scan(parent.getTree(), commit.getTree());
                }
                List<FileChange> changes = new ArrayList<>();
                for (DiffEntry e : entries) {
                    int adds = fmt.toFileHeader(e) == null ? 0 : countLines(e.getNewId().toObjectId(), git).adds();
                    changes.add(new FileChange(e.getChangeType().name(), e.getOldPath(), e.getNewPath(), adds, 0));
                }
                String text = out.toString();
                return new DiffResult(commitId, changes, text);
            }
        } catch (Exception e) {
            throw new NotFoundException("Diff unavailable: " + e.getMessage());
        }
    }

    private record AddDel(int adds) {
    }

    private AddDel countLines(ObjectId newId, Git git) {
        // approximate: count new-side content lines
        try {
            byte[] data = git.getRepository().open(newId).getBytes();
            int c = 0;
            for (byte b : data) {
                if (b == '\n') {
                    c++;
                }
            }
            return new AddDel(c);
        } catch (Exception e) {
            return new AddDel(0);
        }
    }

    public List<FileChange> changedFiles(Git git, RevCommit commit) {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit parsed = walk.parseCommit(commit.getId());
            RevCommit parent = parsed.getParentCount() > 0 ? parsed.getParent(0) : null;
            try (Git inner = Git.open(git.getRepository().getDirectory());
                 DiffFormatter fmt = new DiffFormatter(new ByteArrayOutputStream())) {
                fmt.setRepository(inner.getRepository());
                fmt.setDiffComparator(RawTextComparator.DEFAULT);
                List<DiffEntry> entries = parent == null
                        ? fmt.scan(null, parsed.getTree())
                        : fmt.scan(parent.getTree(), parsed.getTree());
                List<FileChange> out = new ArrayList<>();
                for (DiffEntry e : entries) {
                    out.add(new FileChange(e.getChangeType().name(), e.getOldPath(), e.getNewPath(), 0, 0));
                }
                return out;
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<BlameLine> blame(Long projectId, Long repoId, String filePath) {
        accessService.requireView(projectId);
        GitRepository repo = requireRepo(repoId, projectId);
        try (Git git = Git.open(storageService.repoDirectory(repo).toFile())) {
            var cmd = git.blame().setFilePath(filePath);
            var model = cmd.call();
            List<BlameLine> out = new ArrayList<>();
            for (int i = 0; i < model.getResultContents().size(); i++) {
                RevCommit c = model.getSourceCommit(i);
                PersonIdent a = c == null ? null : c.getAuthorIdent();
                out.add(new BlameLine(i + 1,
                        c == null ? "" : c.getName(),
                        a == null ? "" : a.getName(),
                        c == null ? 0 : c.getCommitTime() * 1000L));
            }
            return out;
        } catch (Exception e) {
            throw new NotFoundException("Blame unavailable: " + e.getMessage());
        }
    }

    private GitRepository requireRepo(Long repoId, Long projectId) {
        return repositoryRepository.findByIdAndProjectId(repoId, projectId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
    }
}