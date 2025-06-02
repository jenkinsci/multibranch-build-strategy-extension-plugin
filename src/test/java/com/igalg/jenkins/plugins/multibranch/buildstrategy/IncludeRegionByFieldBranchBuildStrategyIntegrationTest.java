package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
class IncludeRegionByFieldBranchBuildStrategyIntegrationTest extends IncludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    protected BranchBuildStrategy getBuildStrategy() {
        return new IncludeRegionByFieldBranchBuildStrategy(INCLUDED_REGIONS);
    }
}
