/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 igalg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.toPatterns;

import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMFileSystem;

public class ExcludeRegionByFieldBranchBuildStrategy extends ExcludeRegionBranchBuildStrategy {

    private final String excludedRegions;

    @DataBoundConstructor
    public ExcludeRegionByFieldBranchBuildStrategy(String excludedRegions) {
        this.excludedRegions = excludedRegions;
    }

    @SuppressWarnings("unused") // to keep for UI filling
    public String getExcludedRegions() {
        return excludedRegions;
    }

    @Override
    Set<String> getPatterns(SCMFileSystem fileSystem) {
        return toPatterns(excludedRegions);
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Cancel build by excluded regions strategy";
        }
    }


}
