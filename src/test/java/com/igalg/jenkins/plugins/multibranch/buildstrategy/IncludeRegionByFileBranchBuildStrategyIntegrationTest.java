package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;

public class IncludeRegionByFileBranchBuildStrategyIntegrationTest extends IncludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    BranchBuildStrategy getBuildStrategy() {
        return new IncludeRegionByFileBranchBuildStrategy(".jenkinsIncludeFile");
    }

    @Override
    protected void extendRepoInitialisation() throws Exception {
        sampleGitRepo.write(".jenkinsIncludeFile", INCLUDED_REGIONS);
        sampleGitRepo.git("add", ".jenkinsIncludeFile");
        sampleGitRepo.git("commit", "--all", "--message=add inclusion file");
    }

}
