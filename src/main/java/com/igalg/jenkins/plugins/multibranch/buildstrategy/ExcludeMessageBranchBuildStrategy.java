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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMFileSystem;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ExcludeMessageBranchBuildStrategy extends AbstractMessageBranchBuildStrategy {

    private final String excludedMessages;
    private boolean applyToChangeRequests = true;
    private boolean applyToBranches = true;

    @DataBoundConstructor
    public ExcludeMessageBranchBuildStrategy(String excludedMessages) {
        this.excludedMessages = excludedMessages;
    }

    @SuppressWarnings("unused")
    public String getExcludedMessages() {
        return excludedMessages;
    }

    public boolean isApplyToChangeRequests() {
        return applyToChangeRequests;
    }

    @DataBoundSetter
    public void setApplyToChangeRequests(boolean applyToChangeRequests) {
        this.applyToChangeRequests = applyToChangeRequests;
    }

    public boolean isApplyToBranches() {
        return applyToBranches;
    }

    @DataBoundSetter
    public void setApplyToBranches(boolean applyToBranches) {
        this.applyToBranches = applyToBranches;
    }

    @Override
    Set<String> getPatterns(SCMFileSystem ignored) {
        return Arrays.stream(excludedMessages.split("\n"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    boolean shouldRunBuild(Set<String> patterns, Set<String> singletonMessages, boolean isChangeRequest) {
        // Check if this strategy should apply based on the build type
        if (isChangeRequest && !applyToChangeRequests) {
            return true; // Don't block the build for change requests if not configured to do so
        }
        if (!isChangeRequest && !applyToBranches) {
            return true; // Don't block the build for branches if not configured to do so
        }
        
        // Apply the parent logic if this strategy is configured for this build type
        return super.shouldRunBuild(patterns, singletonMessages, isChangeRequest);
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Cancel build by excluded commit messages strategy";
        }
    }
}
