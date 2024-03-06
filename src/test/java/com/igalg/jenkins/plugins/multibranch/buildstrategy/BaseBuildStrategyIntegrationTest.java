package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import java.io.IOException;
import java.util.Collections;

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchProperty;
import jenkins.branch.BranchSource;
import jenkins.branch.DefaultBranchPropertyStrategy;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.traits.BranchDiscoveryTrait;

abstract class BaseBuildStrategyIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public GitSampleRepoRule sampleGitRepo = new GitSampleRepoRule();

    protected WorkflowMultiBranchProject project;

    @Before
    public void before() throws Exception {
        givenRepoInitialised();
        project = givenMultiBranchProjectCreated();
    }

    private void givenRepoInitialised() throws Exception {
        sampleGitRepo.init();
        sampleGitRepo.write("Jenkinsfile", "node {echo 'hello world'}");
        sampleGitRepo.git("add", "Jenkinsfile");
        sampleGitRepo.git("commit", "--all", "--message=add pipeline");
        extendRepoInitialisation();
        sampleGitRepo.notifyCommit(jenkins);
    }

    protected void extendRepoInitialisation() throws Exception {
        // nothing by default
    }

    private WorkflowMultiBranchProject givenMultiBranchProjectCreated() throws IOException {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "my_project");

        GitSCMSource gitSCMSource = new GitSCMSource(sampleGitRepo.toString());
        gitSCMSource.setTraits(Collections.singletonList(new BranchDiscoveryTrait()));

        BranchSource branchSource = new BranchSource(gitSCMSource);
        DefaultBranchPropertyStrategy branchPropertyStrategy = new DefaultBranchPropertyStrategy(new BranchProperty[0]);
        branchSource.setStrategy(branchPropertyStrategy);
        branchSource.setBuildStrategies(Collections.singletonList(getBuildStrategy()));
        project.getSourcesList().add(branchSource);
        return project;
    }

    abstract BranchBuildStrategy getBuildStrategy();
}
