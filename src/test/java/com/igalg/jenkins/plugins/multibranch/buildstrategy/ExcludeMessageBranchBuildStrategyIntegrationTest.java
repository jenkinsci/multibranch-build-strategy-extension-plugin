package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ExcludeMessageBranchBuildStrategyIntegrationTest extends BaseBuildStrategyIntegrationTest {

    protected static final String EXCLUDED_MESSAGES = ".*\\[ci\\-skip\\].*\n.*\\[maven\\-release\\-plugin\\].*";

    @Override
    BranchBuildStrategy getBuildStrategy() {
        return new ExcludeMessageBranchBuildStrategy(EXCLUDED_MESSAGES);
    }

    @Test
    public void should_notTriggerBuild_when_messageMatchExcludedMessages() throws Exception {
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
    public void should_triggerBuild_when_messageDoesNotMatchExcludedMessages() throws Exception {
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
}
