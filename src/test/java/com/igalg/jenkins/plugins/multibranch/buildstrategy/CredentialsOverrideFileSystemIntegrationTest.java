package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.buildSCMFileSystem;
import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.getGitChangeSetList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;

/**
 * Exercises the Git fetch used when a credential override is set, against a real repository.
 */
@WithJenkins
@WithGitSampleRepo
class CredentialsOverrideFileSystemIntegrationTest {

    private static final String CREDENTIALS_ID = "git-creds";

    private JenkinsRule jenkins;

    private GitSampleRepoRule sampleGitRepo;

    private WorkflowMultiBranchProject project;

    private String baseHash;

    private String changeRequestHash;

    @BeforeEach
    void setUp(JenkinsRule rule, GitSampleRepoRule repo) throws Exception {
        jenkins = rule;
        sampleGitRepo = repo;

        SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, CREDENTIALS_ID, "override", "git-user", "git-password-1234"));
        SystemCredentialsProvider.getInstance().save();

        sampleGitRepo.init();
        baseHash = sampleGitRepo.head();

        // A change request that only exists under refs/pull/1/head, as on GitHub or Bitbucket: there is no
        // refs/heads/PR-1 on the remote.
        sampleGitRepo.git("checkout", "-b", "feature");
        sampleGitRepo.write("src/changed.txt", "changed in pull request");
        sampleGitRepo.git("add", "src/changed.txt");
        sampleGitRepo.git("commit", "--message=change in pull request");
        changeRequestHash = sampleGitRepo.head();
        sampleGitRepo.git("update-ref", "refs/pull/1/head", changeRequestHash);
        sampleGitRepo.git("checkout", "master");
        sampleGitRepo.git("branch", "-D", "feature");

        project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
    }

    private GitSCM changeRequestScm() {
        return new GitSCM(
                List.of(new UserRemoteConfig(sampleGitRepo.toString(), "origin",
                        "+refs/pull/1/head:refs/remotes/origin/PR-1", null)),
                List.of(new BranchSpec("PR-1")),
                null, null, Collections.emptyList());
    }

    private GitSCM branchScm() {
        return new GitSCM(
                List.of(new UserRemoteConfig(sampleGitRepo.toString(), "origin",
                        "+refs/heads/master:refs/remotes/origin/master", null)),
                List.of(new BranchSpec("master")),
                null, null, Collections.emptyList());
    }

    @Test
    void should_fetchChangeRequestRefspec_when_overrideSet() throws Exception {
        SCMHead head = new SCMHead("PR-1");
        SCMRevisionImpl revision = new SCMRevisionImpl(head, changeRequestHash);

        SCMFileSystem fileSystem = buildSCMFileSystem(new GitSCMSource(sampleGitRepo.toString()), head, revision,
                changeRequestScm(), project, CREDENTIALS_ID);

        assertInstanceOf(OverrideCredentialsGitSCMFileSystem.class, fileSystem);
        assertEquals("changed in pull request", fileSystem.getRoot().child("src/changed.txt").contentAsString());

        List<GitChangeSet> changeSets = getGitChangeSetList(fileSystem, head, new SCMRevisionImpl(head, baseHash));
        Set<String> paths = changeSets.stream()
                .flatMap(changeSet -> changeSet.getAffectedPaths().stream())
                .collect(Collectors.toSet());
        assertEquals(Set.of("src/changed.txt"), paths);
    }

    @Test
    void should_failWithGitPluginBuilder_forTheSameChangeRequest() {
        // Documents why the override cannot reuse GitSCMFileSystem.BuilderImpl#build(Item, SCM, SCMRevision):
        // that overload ignores the refspec and fetches refs/heads/PR-1, which does not exist.
        SCMHead head = new SCMHead("PR-1");
        SCMRevisionImpl revision = new SCMRevisionImpl(head, changeRequestHash);

        assertThrows(Exception.class,
                () -> new GitSCMFileSystem.BuilderImpl().build(project, changeRequestScm(), revision));
    }

    @Test
    void should_fetchBranch_when_overrideSet() throws Exception {
        SCMHead head = new SCMHead("master");
        SCMRevisionImpl revision = new SCMRevisionImpl(head, baseHash);

        SCMFileSystem fileSystem = buildSCMFileSystem(new GitSCMSource(sampleGitRepo.toString()), head, revision,
                branchScm(), project, CREDENTIALS_ID);

        assertInstanceOf(OverrideCredentialsGitSCMFileSystem.class, fileSystem);
        assertFalse(fileSystem.getRoot().child("src/changed.txt").exists());
    }

    @Test
    void should_fallBackToSourceCredential_when_overrideCredentialMissing() throws Exception {
        SCMHead head = new SCMHead("master");
        SCMRevisionImpl revision = new SCMRevisionImpl(head, baseHash);

        SCMFileSystem fileSystem = buildSCMFileSystem(new GitSCMSource(sampleGitRepo.toString()), head, revision,
                branchScm(), project, "does-not-exist");

        assertNotNull(fileSystem);
        assertInstanceOf(GitSCMFileSystem.class, fileSystem);
        assertFalse(fileSystem instanceof OverrideCredentialsGitSCMFileSystem);
    }
}
