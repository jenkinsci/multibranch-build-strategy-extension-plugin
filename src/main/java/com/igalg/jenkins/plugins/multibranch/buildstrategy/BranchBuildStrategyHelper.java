/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 igalg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Actionable;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.branch.Branch;
import jenkins.branch.MultiBranchProject;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitRemoteHeadRefAction;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;

final class BranchBuildStrategyHelper {

    private static final Logger LOGGER = Logger.getLogger(BranchBuildStrategyHelper.class.getName());

    private static final int HASH_LENGTH = 40;

    /**
     * Base branch value meaning "the default branch of the repository".
     */
    static final String DEFAULT_BRANCH = "@default";

    private BranchBuildStrategyHelper() {
    }

    static SCMFileSystem buildSCMFileSystem(SCMSource source, SCMHead head, SCMRevision currRevision, SCM scm, SCMSourceOwner owner) throws IOException, InterruptedException {
        SCMFileSystem.Builder builder = new GitSCMFileSystem.BuilderImpl();
        if (currRevision != null && !(currRevision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
            return builder.build(source, head, new AbstractGitSCMSource.SCMRevisionImpl(head, currRevision.toString().substring(0, 40)));
        } else {
            return builder.build(owner, scm, currRevision);
        }
    }

    static List<GitChangeSet> getGitChangeSetList(SCMFileSystem fileSystem, SCMHead head, SCMRevision revision) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (revision != null && !(revision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
            fileSystem.changesSince(new AbstractGitSCMSource.SCMRevisionImpl(head, revision.toString().substring(0, HASH_LENGTH)), out);
        } else {
            fileSystem.changesSince(revision, out);
        }
        GitChangeLogParser parser = new GitChangeLogParser(null, false);
        return parser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Find the merge base of the current revision of a branch and the head of the base branch.
     * <p>
     * Used for a branch without previous build: changes since the merge base are the changes made in the branch
     * (including merges into it), not the whole branch history.
     *
     * @return the merge base revision, or {@code null} if it can not be determined; the caller then falls back to the
     * whole branch history
     */
    @CheckForNull
    static SCMRevision findMergeBaseRevision(SCMSource source, SCMHead head, SCMFileSystem fileSystem, String configuredBaseBranch,
                                             SCMSourceOwner owner, TaskListener listener) throws InterruptedException {
        final String baseBranch = resolveBaseBranch(configuredBaseBranch, source, owner);
        if (baseBranch == null) {
            listener.getLogger().printf("Default branch is not known yet, %s is compared against its whole history%n", head.getName());
            return null;
        }
        if (head.getName().equals(baseBranch)) {
            // comparing the base branch with itself makes no sense
            LOGGER.fine(() -> String.format("Branch %s is the base branch, comparing against the whole history", baseBranch));
            return null;
        }
        final AbstractGitSCMSource.SCMRevisionImpl currRevision =
                fileSystem instanceof GitSCMFileSystem gitFs ? gitFs.getRevision() : null;
        if (currRevision == null) {
            LOGGER.fine(() -> String.format("No git revision for branch %s, comparing against the whole history", head.getName()));
            return null;
        }

        try {
            final SCMHead baseHead = new GitBranchSCMHead(baseBranch);
            final SCMRevision baseRevision = source.fetch(baseHead, listener);
            if (!(baseRevision instanceof AbstractGitSCMSource.SCMRevisionImpl baseRevisionImpl)) {
                listener.getLogger().printf("Base branch %s not found, %s is compared against its whole history%n", baseBranch, head.getName());
                return null;
            }

            final ObjectId baseId = ObjectId.fromString(baseRevisionImpl.getHash());
            final ObjectId currId = ObjectId.fromString(currRevision.getHash());
            final GitSCMFileSystem gitFileSystem = (GitSCMFileSystem) fileSystem;

            // The file system of the branch fetches only this branch into the cache, make sure the base is there too.
            if (!Boolean.TRUE.equals(gitFileSystem.invoke(repository -> repository.getObjectDatabase().has(baseId)))) {
                buildSCMFileSystem(source, baseHead, baseRevision, source.build(baseHead, baseRevision), owner);
            }

            final ObjectId mergeBase = gitFileSystem.invoke(repository -> mergeBase(repository, currId, baseId));
            if (mergeBase == null) {
                listener.getLogger().printf("No merge base of %s and %s, %s is compared against its whole history%n", head.getName(), baseBranch, head.getName());
                return null;
            }

            listener.getLogger().printf("%s has no previous build, comparing against merge base %s with %s%n", head.getName(), mergeBase.name(), baseBranch);
            return new AbstractGitSCMSource.SCMRevisionImpl(head, mergeBase.name());
        } catch (IOException | RuntimeException e) {
            LOGGER.warning(() -> String.format("Unable to find merge base of %s and %s: %s", head.getName(), baseBranch, e));
            listener.getLogger().printf("Unable to find merge base of %s and %s, %s is compared against its whole history: %s%n", head.getName(), baseBranch, head.getName(), e);
            return null;
        }
    }

    /**
     * Resolve the configured base branch: a branch name is used as is, {@value #DEFAULT_BRANCH} is resolved to the
     * default branch of the repository known from the last branch indexing.
     *
     * @return the base branch name, or {@code null} if the default branch is not known
     */
    @CheckForNull
    static String resolveBaseBranch(String configuredBaseBranch, SCMSource source, SCMSourceOwner owner) {
        if (!DEFAULT_BRANCH.equals(configuredBaseBranch)) {
            return configuredBaseBranch;
        }

        // Git sources: the remote HEAD fetched by the branch indexing, stored on the multibranch project.
        if (owner instanceof Actionable actionable && source instanceof AbstractGitSCMSource gitSource) {
            for (GitRemoteHeadRefAction action : actionable.getActions(GitRemoteHeadRefAction.class)) {
                if (gitSource.getRemote().equals(action.getRemote())) {
                    return action.getName();
                }
            }
        }

        // Other sources (GitHub, GitLab, Bitbucket...) mark the branch of the default branch as primary.
        if (owner instanceof MultiBranchProject<?, ?> project) {
            for (Object item : project.getItems()) {
                final Branch branch = branchOf(project, item);
                if (branch != null && source.getId().equals(branch.getSourceId())
                        && branch.getAction(PrimaryInstanceMetadataAction.class) != null) {
                    return branch.getHead().getName();
                }
            }
        }

        LOGGER.fine(() -> "Default branch not found for source " + source.getId());
        return null;
    }

    @CheckForNull
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Branch branchOf(MultiBranchProject project, Object item) {
        if (!project.getProjectFactory().isProject((Item) item)) {
            return null;
        }
        return project.getProjectFactory().getBranch((Job) item);
    }

    @CheckForNull
    static ObjectId mergeBase(Repository repository, ObjectId first, ObjectId second) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(walk.parseCommit(first));
            walk.markStart(walk.parseCommit(second));
            final RevCommit mergeBase = walk.next();
            return mergeBase == null ? null : mergeBase.copy();
        }
    }

    /**
     * Reads the patterns from a file at the root of the SCM.
     * <p>
     * A missing file (or a path that is not a file) yields an empty set. Any failure while looking up or reading the
     * file is propagated, so that {@code AbstractBranchBuildStrategy#isAutomaticBuild} can fail open instead of
     * mistaking an infrastructure error for "no patterns".
     */
    static Set<String> getPatternsFromFile(SCMFileSystem fileSystem, String filePath)
            throws IOException, InterruptedException {
        LOGGER.info(() -> String.format("Looking for file: %s", filePath));

        final SCMFile ignorefile = fileSystem.getRoot().child(filePath);
        if (!ignorefile.exists() || !ignorefile.isFile()) {
            LOGGER.severe(() -> String.format("File: %s not found", filePath));
            return Collections.emptySet();
        }

        return toPatterns(ignorefile.contentAsString());
    }

    static Set<String> toPatterns(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        return Arrays.stream(value.split("\n"))
                .filter(p -> !p.startsWith("#"))
                .filter(StringUtils::isNotBlank)
                .map(row -> {
                    // path from ChangeSet does not start with "/"
                    if (row.startsWith("/")) {
                        return row.substring(1);
                    }
                    return row;
                })
                .map(String::trim)
                .collect(Collectors.toSet());
    }

}
