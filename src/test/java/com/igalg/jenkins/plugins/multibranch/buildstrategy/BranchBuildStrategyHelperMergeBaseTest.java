package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.findMergeBaseRevision;
import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.mergeBase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;

@ExtendWith(MockitoExtension.class)
class BranchBuildStrategyHelperMergeBaseTest {

    private final SCMHead head = new GitBranchSCMHead("feature");

    private final TaskListener listener = StreamTaskListener.fromStdout();

    @TempDir
    private File directory;

    @Mock
    private GitSCMSource source;

    @Mock
    private SCMSourceOwner owner;

    @Mock
    private GitSCMFileSystem fileSystem;

    private Git git;

    private RevCommit forkPoint;

    private RevCommit masterTip;

    private RevCommit featureTip;

    @BeforeEach
    void setUp() throws Exception {
        // master: forkPoint - masterTip
        // feature:          \ featureTip
        git = Git.init().setDirectory(directory).setInitialBranch("master").call();
        forkPoint = commit("first");
        masterTip = commit("master change");
        git.checkout().setCreateBranch(true).setName("feature").setStartPoint(forkPoint).call();
        featureTip = commit("feature change");
    }

    @AfterEach
    void tearDown() {
        git.close();
    }

    @Test
    void should_returnForkPoint_when_branchesDiverged() throws IOException {
        assertEquals(forkPoint, mergeBase(git.getRepository(), featureTip, masterTip));
    }

    @Test
    void should_returnBranchTip_when_branchHasNoCommits() throws IOException {
        assertEquals(forkPoint, mergeBase(git.getRepository(), forkPoint, masterTip));
    }

    @Test
    void should_returnNull_when_historiesAreUnrelated() throws Exception {
        // given
        git.checkout().setOrphan(true).setName("orphan").call();
        RevCommit orphan = commit("unrelated");

        // then
        assertNull(mergeBase(git.getRepository(), orphan, masterTip));
    }

    @Test
    void should_returnMergeBaseRevision() throws Exception {
        // given
        givenFileSystemOf(featureTip);
        given(source.fetch(new GitBranchSCMHead("master"), listener)).willReturn(new SCMRevisionImpl(new GitBranchSCMHead("master"), masterTip.name()));

        // when
        SCMRevision revision = findMergeBaseRevision(source, head, fileSystem, "master", owner, listener);

        // then
        assertEquals(new SCMRevisionImpl(head, forkPoint.name()), revision);
    }

    @Test
    void should_returnNull_when_branchIsTheBaseBranch() throws Exception {
        // when
        SCMRevision revision = findMergeBaseRevision(source, new GitBranchSCMHead("master"), fileSystem, "master", owner, listener);

        // then
        assertNull(revision);
        then(source).should(never()).fetch(any(SCMHead.class), any(TaskListener.class));
    }

    @Test
    void should_returnNull_when_fileSystemIsNotGit() throws Exception {
        // when
        SCMRevision revision = findMergeBaseRevision(source, head, mock(SCMFileSystem.class), "master", owner, listener);

        // then
        assertNull(revision);
        then(source).should(never()).fetch(any(SCMHead.class), any(TaskListener.class));
    }

    @Test
    void should_returnNull_when_baseBranchNotFound() throws Exception {
        // given
        given(fileSystem.getRevision()).willReturn(new SCMRevisionImpl(head, featureTip.name()));
        given(source.fetch(new GitBranchSCMHead("missing"), listener)).willReturn(null);

        // when
        SCMRevision revision = findMergeBaseRevision(source, head, fileSystem, "missing", owner, listener);

        // then
        assertNull(revision);
    }

    @Test
    void should_returnNull_when_fetchFails() throws Exception {
        // given
        given(fileSystem.getRevision()).willReturn(new SCMRevisionImpl(head, featureTip.name()));
        given(source.fetch(new GitBranchSCMHead("master"), listener)).willThrow(new IOException("unreachable"));

        // when
        SCMRevision revision = findMergeBaseRevision(source, head, fileSystem, "master", owner, listener);

        // then
        assertNull(revision);
    }

    @Test
    void should_returnNull_when_noMergeBase() throws Exception {
        // given
        git.checkout().setOrphan(true).setName("orphan").call();
        RevCommit orphan = commit("unrelated");
        givenFileSystemOf(orphan);
        given(source.fetch(new GitBranchSCMHead("master"), listener)).willReturn(new SCMRevisionImpl(new GitBranchSCMHead("master"), masterTip.name()));

        // when
        SCMRevision revision = findMergeBaseRevision(source, head, fileSystem, "master", owner, listener);

        // then
        assertNull(revision);
    }

    private void givenFileSystemOf(ObjectId commit) throws Exception {
        given(fileSystem.getRevision()).willReturn(new SCMRevisionImpl(head, commit.name()));
        given(fileSystem.invoke(any())).willAnswer(invocation ->
                invocation.<GitSCMFileSystem.FSFunction<?>>getArgument(0).invoke(git.getRepository()));
    }

    private RevCommit commit(String message) throws Exception {
        return git.commit().setMessage(message).setAllowEmpty(true).setSign(false).call();
    }
}
