package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;

public class IncludeRegionByFieldBranchBuildStrategyIntegrationTest extends IncludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    BranchBuildStrategy getBuildStrategy() {
        return new IncludeRegionByFieldBranchBuildStrategy(INCLUDED_REGIONS);
    }
}
