package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * The default branch of the repository (its {@code HEAD}) is {@code develop}, not {@code master}.
 * Branches without previous build are compared against their merge base with it.
 */
@WithJenkins
@WithGitSampleRepo
class IncludeRegionCompareWithDefaultBranchIntegrationTest extends BaseBuildStrategyIntegrationTest {

    @Override
    protected void extendRepoInitialisation() throws Exception {
        commit("foo/file", "initial module");
        sampleGitRepo.git("checkout", "-b", "develop");
        commit("foo/develop", "module change in develop");
    }

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        IncludeRegionByFieldBranchBuildStrategy strategy = new IncludeRegionByFieldBranchBuildStrategy("foo/**");
        strategy.setCompareNewBranchesWith("@default");
        return strategy;
    }

    @Test
    void should_compareAgainstDefaultBranch() throws Exception {
        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then the default branch is checked against its whole history
        assertBuilds("develop", 1);
        // and master, behind develop, has no changes of its own
        assertBuilds("master", 0);
    }

    @Test
    void should_NotTriggerBuild_when_newBranchHasNoCommits() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("branch", "feature");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 0);
    }

    @Test
    void should_triggerBuild_when_newBranchHasCommitInIncludedRegion() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        commit("foo/feature", "module change");
        // the checked out branch is the remote HEAD, keep develop as the default branch
        sampleGitRepo.git("checkout", "develop");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 1);
    }

    private void commit(String file, String message) throws Exception {
        sampleGitRepo.write(file, message);
        sampleGitRepo.git("add", file);
        sampleGitRepo.git("commit", "--all", "--message=" + message);
    }

    private void assertBuilds(String branch, int count) {
        WorkflowJob job = project.getItem(branch);
        assertNotNull(job, "branch " + branch + " is not indexed");
        if (count == 0) {
            assertNull(job.getLastBuild(), "branch " + branch + " should not be built");
        } else {
            assertNotNull(job.getLastBuild(), "branch " + branch + " should be built");
            assertEquals(count, job.getLastBuild().getNumber(), "builds of " + branch);
        }
    }
}
