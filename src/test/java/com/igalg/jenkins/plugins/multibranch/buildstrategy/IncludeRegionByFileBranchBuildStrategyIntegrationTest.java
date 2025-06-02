package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
class IncludeRegionByFileBranchBuildStrategyIntegrationTest extends IncludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        return new IncludeRegionByFileBranchBuildStrategy(".jenkinsIncludeFile");
    }

    @Override
    protected void extendRepoInitialisation() throws Exception {
        sampleGitRepo.write(".jenkinsIncludeFile", INCLUDED_REGIONS);
        sampleGitRepo.git("add", ".jenkinsIncludeFile");
        sampleGitRepo.git("commit", "--all", "--message=add inclusion file");
    }
}
