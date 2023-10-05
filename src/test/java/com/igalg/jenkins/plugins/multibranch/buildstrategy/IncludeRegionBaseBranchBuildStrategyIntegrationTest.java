package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;

abstract class IncludeRegionBaseBranchBuildStrategyIntegrationTest extends BaseBuildStrategyIntegrationTest{

    protected static final String INCLUDED_REGIONS = "foo/**";

    @Test
    public void should_NotTriggerBuild_when_fileDoesNotMatchIncludeRegion() throws Exception {
        // given a new file not in included region
        sampleGitRepo.write("bar/file", "some content");
        sampleGitRepo.git("add", "bar/file");
        sampleGitRepo.git("commit", "--all", "--message=KO");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then no new build is triggered
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        assertNull(job.getLastBuild());
    }

    @Test
    public void should_triggerBuild_when_fileDoesMatchIncludeRegion() throws Exception {
        // given a new file in included region
        sampleGitRepo.write("foo/file", "some content");
        sampleGitRepo.git("add", "foo/file");
        sampleGitRepo.git("commit", "--all", "--message=OK");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then new build is triggered
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        jenkins.assertBuildStatusSuccess(job.getLastBuild());
    }
}
