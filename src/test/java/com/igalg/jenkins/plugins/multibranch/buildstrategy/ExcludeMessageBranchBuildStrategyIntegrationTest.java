package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
class ExcludeMessageBranchBuildStrategyIntegrationTest extends BaseBuildStrategyIntegrationTest {

    protected static final String EXCLUDED_MESSAGES = ".*\\[ci\\-skip\\].*\n.*\\[maven\\-release\\-plugin\\].*";

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        return new ExcludeMessageBranchBuildStrategy(EXCLUDED_MESSAGES);
    }

    @Test
    void should_notTriggerBuild_when_messageMatchExcludedMessages() throws Exception {
        // given
        sampleGitRepo.write("exampleFile", "Example file content");
        sampleGitRepo.git("add", "exampleFile");
        sampleGitRepo.git("commit", "--message=[ci-skip] Example commit message");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        assertNull(job.getLastBuild());
    }

    @Test
    void should_triggerBuild_when_messageDoesNotMatchExcludedMessages() throws Exception {
        // given
        sampleGitRepo.write("exampleFile", "Example file content");
        sampleGitRepo.git("add", "exampleFile");
        sampleGitRepo.git("commit", "--message=Example commit message");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        jenkins.assertBuildStatusSuccess(job.getLastBuild());
    }

    @Test
    void should_triggerBuild_when_applyToBranchesIsFalse_and_messageMatchesPattern() throws Exception {
        // given - strategy configured to NOT apply to branches
        ExcludeMessageBranchBuildStrategy strategy = new ExcludeMessageBranchBuildStrategy(EXCLUDED_MESSAGES);
        strategy.setApplyToChangeRequests(true);
        strategy.setApplyToBranches(false);
        
        jenkins.branch.BranchSource branchSource = project.getSourcesList().get(0);
        branchSource.setBuildStrategies(java.util.Collections.singletonList(strategy));

        sampleGitRepo.write("exampleFile", "Example file content");
        sampleGitRepo.git("add", "exampleFile");
        sampleGitRepo.git("commit", "--message=[ci-skip] Example commit message");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then - build should be triggered because applyToBranches is false
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        jenkins.assertBuildStatusSuccess(job.getLastBuild());
    }

    @Test
    void should_notTriggerBuild_when_applyToBranchesIsTrue_and_messageMatchesPattern() throws Exception {
        // given - strategy configured to apply to branches
        ExcludeMessageBranchBuildStrategy strategy = new ExcludeMessageBranchBuildStrategy(EXCLUDED_MESSAGES);
        // Default values are true, true - no need to set explicitly
        
        jenkins.branch.BranchSource branchSource = project.getSourcesList().get(0);
        branchSource.setBuildStrategies(java.util.Collections.singletonList(strategy));

        sampleGitRepo.write("exampleFile", "Example file content");
        sampleGitRepo.git("add", "exampleFile");
        sampleGitRepo.git("commit", "--message=[ci-skip] Example commit message");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then - build should NOT be triggered because applyToBranches is true
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        assertNull(job.getLastBuild());
    }
}
