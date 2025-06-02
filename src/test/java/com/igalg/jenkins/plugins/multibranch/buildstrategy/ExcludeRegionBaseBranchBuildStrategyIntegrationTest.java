package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
abstract class ExcludeRegionBaseBranchBuildStrategyIntegrationTest extends BaseBuildStrategyIntegrationTest {

    protected static final String EXCLUDED_REGIONS = "file\nJenkinsfile\nfoo/**";

    @Test
    void should_NotTriggerBuild_when_fileMatchExcludeRegion() throws Exception {
        // given
        sampleGitRepo.write("foo/file", "some content");
        sampleGitRepo.git("add", "foo/file");
        sampleGitRepo.git("commit", "--all", "--message=KO");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        assertNull(job.getLastBuild());
    }

    @Test
    void should_triggerBuild_when_fileDoesNotMatchExcludeRegion() throws Exception {
        // given
        sampleGitRepo.write("bar/file", "some content");
        sampleGitRepo.git("add", "bar/file");
        sampleGitRepo.git("commit", "--all", "--message=OK");

        // when
        sampleGitRepo.notifyCommit(jenkins);

        // then
        WorkflowJob job = project.getItem("master");
        assertNotNull(job);
        jenkins.assertBuildStatusSuccess(job.getLastBuild());
    }
}
