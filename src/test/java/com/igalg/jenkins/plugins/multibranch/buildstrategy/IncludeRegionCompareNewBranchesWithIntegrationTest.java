package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Branches without previous build are compared against their merge base with the {@code master} branch.
 * The history of {@code master} already touches the included region, so comparing against the whole history would
 * build every new branch.
 */
@WithJenkins
@WithGitSampleRepo
class IncludeRegionCompareNewBranchesWithIntegrationTest extends BaseBuildStrategyIntegrationTest {

    private static final String INCLUDED_REGIONS = "foo/**";

    @Override
    protected void extendRepoInitialisation() throws Exception {
        commit("foo/file", "initial module");
    }

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        IncludeRegionByFieldBranchBuildStrategy strategy = new IncludeRegionByFieldBranchBuildStrategy(INCLUDED_REGIONS);
        strategy.setCompareNewBranchesWith("master");
        return strategy;
    }

    @Test
    void should_compareBaseBranchAgainstWholeHistory() throws Exception {
        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then the base branch is not compared with itself
        assertBuilds("master", 1);
    }

    @Test
    void should_NotTriggerBuild_when_newBranchHasNoCommits() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 0);
    }

    @Test
    void should_NotTriggerBuild_when_newBranchCreatedFromOutdatedBase() throws Exception {
        // given a branch created from an older master
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        sampleGitRepo.git("checkout", "master");
        commit("foo/other", "module change in master");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then the master changes are not changes of the branch
        assertBuilds("master", 2);
        assertBuilds("feature", 0);
    }

    @Test
    void should_triggerBuild_when_newBranchHasCommitInIncludedRegion() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        commit("foo/feature", "module change");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 1);
    }

    @Test
    void should_NotTriggerBuild_when_newBranchHasCommitOutsideIncludedRegion() throws Exception {
        // given
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        commit("bar/feature", "other change");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 0);
    }

    @Test
    void should_triggerBuild_when_parameterNotSet() throws Exception {
        // given the default behaviour
        strategy().setCompareNewBranchesWith(null);
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then the whole history is checked
        assertBuilds("feature", 1);
    }

    @Test
    void should_triggerBuild_when_baseBranchDoesNotExist() throws Exception {
        // given
        strategy().setCompareNewBranchesWith("missing");
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then falls back to the whole history
        assertBuilds("feature", 1);
    }

    @Test
    void should_NotTriggerBuild_when_builtBranchHasCommitOutsideIncludedRegion() throws Exception {
        // given a branch with a previous build
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        commit("foo/feature", "module change");
        sampleGitRepo.notifyCommit(jenkins);
        assertBuilds("feature", 1);

        commit("bar/feature", "other change");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        assertBuilds("feature", 1);
    }

    @Test
    void should_triggerBuild_when_baseBranchWithIncludedRegionChangeMergedIntoBuiltBranch() throws Exception {
        // given a branch with a previous build
        sampleGitRepo.notifyCommit(jenkins);
        sampleGitRepo.git("checkout", "-b", "feature");
        commit("foo/feature", "module change");
        sampleGitRepo.notifyCommit(jenkins);
        assertBuilds("feature", 1);

        sampleGitRepo.git("checkout", "master");
        commit("foo/other", "module change in master");
        sampleGitRepo.git("checkout", "feature");
        sampleGitRepo.git("merge", "--no-ff", "--message=merge master", "master");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then the merge is a change of the branch
        assertBuilds("feature", 2);
    }

    @Test
    void should_keepParameter_when_configurationSaved() throws Exception {
        // when
        jenkins.configRoundtrip(project);

        // then
        assertEquals("master", strategy().getCompareNewBranchesWith());
        assertEquals(INCLUDED_REGIONS, strategy().getIncludedRegions());
    }

    @Test
    void should_instantiateFromDescribableModel() throws Exception {
        // when configured the way Job DSL and pipeline steps do
        IncludeRegionByFieldBranchBuildStrategy strategy = new DescribableModel<>(IncludeRegionByFieldBranchBuildStrategy.class)
                .instantiate(Map.of("includedRegions", INCLUDED_REGIONS, "compareNewBranchesWith", "master"));

        // then
        assertEquals("master", strategy.getCompareNewBranchesWith());
    }

    private void commit(String file, String message) throws Exception {
        sampleGitRepo.write(file, message);
        sampleGitRepo.git("add", file);
        sampleGitRepo.git("commit", "--all", "--message=" + message);
    }

    private IncludeRegionByFieldBranchBuildStrategy strategy() {
        BranchSource source = project.getSourcesList().get(0);
        return (IncludeRegionByFieldBranchBuildStrategy) source.getBuildStrategies().get(0);
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
