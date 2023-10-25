package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import jenkins.branch.BranchBuildStrategy;

public class ExcludeRegionByFieldBranchBuildStrategyIntegrationTest extends ExcludeRegionBaseBranchBuildStrategyIntegrationTest {

    @Override
    BranchBuildStrategy getBuildStrategy() {
        return new ExcludeRegionByFieldBranchBuildStrategy(EXCLUDED_REGIONS);
    }

}
