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

import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Util;
import hudson.plugins.git.GitChangeSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractRegionBranchBuildStrategy extends AbstractBranchBuildStrategy {

    private static final Logger LOGGER = Logger.getLogger(AbstractRegionBranchBuildStrategy.class.getName());

    private String compareNewBranchesWith;

    protected AbstractRegionBranchBuildStrategy(Strategy strategy) {
        super(strategy);
    }

    /**
     * @return name of the branch to compare a branch without previous build against, or {@code null}
     */
    @CheckForNull
    public String getCompareNewBranchesWith() {
        return compareNewBranchesWith;
    }

    @DataBoundSetter
    public void setCompareNewBranchesWith(String compareNewBranchesWith) {
        this.compareNewBranchesWith = Util.fixEmptyAndTrim(compareNewBranchesWith);
    }

    @Override
    String getBaseBranchForNewBranches() {
        return compareNewBranchesWith;
    }

    Set<String> getExpressions(List<GitChangeSet> changeSets) {
        final Set<String> paths = new HashSet<>();
        for (GitChangeSet changeSet : changeSets) {
            Collection<GitChangeSet.Path> affectedFiles = changeSet.getAffectedFiles();
            for (GitChangeSet.Path path : affectedFiles) {
                paths.add(path.getPath());
                LOGGER.fine(() -> "File: " + path.getPath() + " from commit: " + changeSet.getCommitId() + "; change type:" + path.getEditType().getName());
            }
        }

        return paths;
    }
}
