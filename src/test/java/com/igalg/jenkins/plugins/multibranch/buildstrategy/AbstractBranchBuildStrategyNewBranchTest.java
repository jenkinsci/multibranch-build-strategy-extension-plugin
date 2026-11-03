package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.AbstractBranchBuildStrategy.Strategy.INCLUDED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.mixin.ChangeRequestSCMRevision;

@ExtendWith(MockitoExtension.class)
class AbstractBranchBuildStrategyNewBranchTest {

    static class TestBranchBuildStrategy extends AbstractBranchBuildStrategy {

        private final String baseBranch;

        TestBranchBuildStrategy(String baseBranch) {
            super(INCLUDED);
            this.baseBranch = baseBranch;
        }

        @Override
        String getBaseBranchForNewBranches() {
            return baseBranch;
        }

        @Override
        Set<String> getPatterns(SCMFileSystem fileSystem) {
            return Set.of("foo/**");
        }

        @Override
        Set<String> getExpressions(List<GitChangeSet> changeSets) {
            return Set.of();
        }

        @Override
        boolean shouldRunBuild(Set<String> patterns, Set<String> expressions, boolean isChangeRequest) {
            return false;
        }
    }

    @Mock
    private SCMHead head;

    @Mock
    private GitSCMSource source;

    @Mock
    private SCMRevisionImpl currRevision;

    @Mock
    private SCMRevisionImpl lastBuiltRevision;

    @Mock
    private TaskListener listener;

    @Mock
    private SCMSourceOwner owner;

    @Mock
    private SCM scm;

    @Mock
    private GitSCMFileSystem fileSystem;

    @BeforeEach
    void setUp() {
        given(source.getOwner()).willReturn(owner);
    }

    @Test
    void should_compareAgainstMergeBase_when_branchHasNoPreviousBuild() {
        // given
        SCMRevision mergeBase = mock(SCMRevisionImpl.class);
        given(source.build(head, currRevision)).willReturn(scm);

        try (MockedStatic<BranchBuildStrategyHelper> helper = mockStatic(BranchBuildStrategyHelper.class)) {
            helper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);
            helper.when(() -> BranchBuildStrategyHelper.findMergeBaseRevision(source, head, fileSystem, "master", owner, listener)).thenReturn(mergeBase);

            // when
            new TestBranchBuildStrategy("master").isAutomaticBuild(source, head, currRevision, null, null, listener);

            // then
            helper.verify(() -> BranchBuildStrategyHelper.getGitChangeSetList(fileSystem, head, mergeBase));
        }
    }

    @Test
    void should_compareAgainstWholeHistory_when_mergeBaseNotFound() {
        // given
        given(source.build(head, currRevision)).willReturn(scm);

        try (MockedStatic<BranchBuildStrategyHelper> helper = mockStatic(BranchBuildStrategyHelper.class)) {
            helper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);
            helper.when(() -> BranchBuildStrategyHelper.findMergeBaseRevision(source, head, fileSystem, "master", owner, listener)).thenReturn(null);

            // when
            new TestBranchBuildStrategy("master").isAutomaticBuild(source, head, currRevision, null, null, listener);

            // then
            helper.verify(() -> BranchBuildStrategyHelper.getGitChangeSetList(fileSystem, head, null));
        }
    }

    @Test
    void should_notLookForMergeBase_when_baseBranchNotConfigured() {
        // given
        given(source.build(head, currRevision)).willReturn(scm);

        try (MockedStatic<BranchBuildStrategyHelper> helper = mockStatic(BranchBuildStrategyHelper.class)) {
            helper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            new TestBranchBuildStrategy(null).isAutomaticBuild(source, head, currRevision, null, null, listener);

            // then
            helper.verify(() -> BranchBuildStrategyHelper.findMergeBaseRevision(any(), any(), any(), any(), any(), any()), never());
            helper.verify(() -> BranchBuildStrategyHelper.getGitChangeSetList(fileSystem, head, null));
        }
    }

    @Test
    void should_notLookForMergeBase_when_branchHasPreviousBuild() {
        // given
        given(source.build(head, currRevision)).willReturn(scm);

        try (MockedStatic<BranchBuildStrategyHelper> helper = mockStatic(BranchBuildStrategyHelper.class)) {
            helper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            new TestBranchBuildStrategy("master").isAutomaticBuild(source, head, currRevision, lastBuiltRevision, null, listener);

            // then
            helper.verify(() -> BranchBuildStrategyHelper.findMergeBaseRevision(any(), any(), any(), any(), any(), any()), never());
            helper.verify(() -> BranchBuildStrategyHelper.getGitChangeSetList(fileSystem, head, lastBuiltRevision));
        }
    }

    @Test
    void should_compareAgainstTarget_when_changeRequestHasNoPreviousBuild() {
        // given
        ChangeRequestSCMRevision<?> changeRequest = mock(ChangeRequestSCMRevision.class);
        SCMRevision target = mock(SCMRevisionImpl.class);
        given(changeRequest.getTarget()).willReturn(target);
        given(source.build(head, changeRequest)).willReturn(scm);

        try (MockedStatic<BranchBuildStrategyHelper> helper = mockStatic(BranchBuildStrategyHelper.class)) {
            helper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, changeRequest, scm, owner)).thenReturn(fileSystem);

            // when
            new TestBranchBuildStrategy("master").isAutomaticBuild(source, head, changeRequest, null, null, listener);

            // then
            helper.verify(() -> BranchBuildStrategyHelper.findMergeBaseRevision(any(), any(), any(), any(), any(), any()), never());
            helper.verify(() -> BranchBuildStrategyHelper.getGitChangeSetList(fileSystem, head, target));
        }
    }
}
