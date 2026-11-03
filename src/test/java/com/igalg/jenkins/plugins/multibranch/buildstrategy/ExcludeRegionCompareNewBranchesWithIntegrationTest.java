package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * The history of {@code master} contains files outside the excluded region, so comparing a new branch against the
 * whole history would build it.
 */
@WithJenkins
@WithGitSampleRepo
class ExcludeRegionCompareNewBranchesWithIntegrationTest extends BaseBuildStrategyIntegrationTest {

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        ExcludeRegionByFieldBranchBuildStrategy strategy = new ExcludeRegionByFieldBranchBuildStrategy("foo/**");
        strategy.setCompareNewBranchesWith("master");
        return strategy;
    }

    @Test
    void should_NotTriggerBuild_when_newBranchHasNoCommits() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("feature");
        assertNotNull(job);
        assertNull(job.getLastBuild());
    }

    @Test
    void should_triggerBuild_when_newBranchHasCommitOutsideExcludedRegion() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        sampleGitRepo.write("bar/file", "some content");
        sampleGitRepo.git("add", "bar/file");
        sampleGitRepo.git("commit", "--all", "--message=OK");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("feature");
        assertNotNull(job);
        jenkins.assertBuildStatusSuccess(job.getLastBuild());
    }
}
