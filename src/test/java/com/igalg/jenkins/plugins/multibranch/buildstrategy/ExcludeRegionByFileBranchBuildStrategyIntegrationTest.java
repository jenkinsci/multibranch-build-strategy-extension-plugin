package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
class ExcludeRegionByFileBranchBuildStrategyIntegrationTest extends ExcludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        return new ExcludeRegionByFileBranchBuildStrategy(".jenkinsExcludeFile");
    }

    @Override
    protected void extendRepoInitialisation() throws Exception {
        sampleGitRepo.write(".jenkinsExcludeFile", EXCLUDED_REGIONS + "\n.jenkinsExcludeFile");
        sampleGitRepo.git("add", ".jenkinsExcludeFile");
        sampleGitRepo.git("commit", "--all", "--message=add exclusion file");
    }
}
