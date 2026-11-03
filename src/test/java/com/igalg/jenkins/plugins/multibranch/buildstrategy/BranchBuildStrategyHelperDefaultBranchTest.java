package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.resolveBaseBranch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jenkins.branch.Branch;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitRemoteHeadRefAction;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;

@ExtendWith(MockitoExtension.class)
class BranchBuildStrategyHelperDefaultBranchTest {

    private static final String REMOTE = "https://git.example.com/group/repo.git";

    @Mock
    private GitSCMSource source;

    @Mock
    private WorkflowMultiBranchProject project;

    @Test
    void should_returnBranchName_when_notDefault() {
        assertEquals("release", resolveBaseBranch("release", source, project));
    }

    @Test
    void should_returnRemoteHead_when_known() {
        // given
        given(source.getRemote()).willReturn(REMOTE);
        given(project.getActions(GitRemoteHeadRefAction.class)).willReturn(List.of(new GitRemoteHeadRefAction(REMOTE, "develop")));

        // then
        assertEquals("develop", resolveBaseBranch("@default", source, project));
    }

    @Test
    void should_returnPrimaryBranch_when_remoteHeadUnknown() {
        // given
        WorkflowJob job = mock(WorkflowJob.class);
        WorkflowBranchProjectFactory factory = mock(WorkflowBranchProjectFactory.class);
        Branch branch = mock(Branch.class);
        given(source.getRemote()).willReturn(REMOTE);
        given(source.getId()).willReturn("source-id");
        given(project.getActions(GitRemoteHeadRefAction.class)).willReturn(List.of(new GitRemoteHeadRefAction("https://other/repo.git", "develop")));
        given(project.getItems()).willReturn(List.of(job));
        given(project.getProjectFactory()).willReturn(factory);
        given(factory.isProject(job)).willReturn(true);
        given(factory.getBranch(job)).willReturn(branch);
        given(branch.getSourceId()).willReturn("source-id");
        given(branch.getAction(PrimaryInstanceMetadataAction.class)).willReturn(new PrimaryInstanceMetadataAction());
        given(branch.getHead()).willReturn(new GitBranchSCMHead("main"));

        // then
        assertEquals("main", resolveBaseBranch("@default", source, project));
    }

    @Test
    void should_returnNull_when_defaultBranchUnknown() {
        // given
        given(project.getActions(GitRemoteHeadRefAction.class)).willReturn(List.of());
        given(project.getItems()).willReturn(List.of());

        // then
        assertNull(resolveBaseBranch("@default", source, project));
    }

    @Test
    void should_returnNull_when_ownerIsNotMultiBranchProject() {
        assertNull(resolveBaseBranch("@default", source, mock(SCMSourceOwner.class)));
    }
}
