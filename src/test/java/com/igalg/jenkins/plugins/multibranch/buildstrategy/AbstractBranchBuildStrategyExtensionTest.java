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

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.AbstractBranchBuildStrategy.Strategy.EXCLUDED;
import static com.igalg.jenkins.plugins.multibranch.buildstrategy.AbstractBranchBuildStrategy.Strategy.INCLUDED;
import static jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.SCM;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestSCMRevision;

@RunWith(MockitoJUnitRunner.class)
public class AbstractBranchBuildStrategyExtensionTest {

    static class TestBranchBuildStrategy extends AbstractBranchBuildStrategy {

        private final Set<String> patterns;

        private final Set<String> expressions;

        boolean isShouldRunBuildExecuted = false;

        protected TestBranchBuildStrategy(Strategy strategy, Set<String> patterns, Set<String> expressions) {
            super(strategy);
            this.patterns = patterns;
            this.expressions = expressions;
        }

        @Override
        Set<String> getPatterns(SCMFileSystem fileSystem) {
            return patterns;
        }

        @Override
        Set<String> getExpressions(List<GitChangeSet> changeSets) {
            return expressions;
        }

        @Override
        boolean shouldRunBuild(Set<String> patterns, Set<String> expressions) {
            isShouldRunBuildExecuted = true;
            return false;
        }
    }

    @Mock
    protected SCMHead head;

    @Mock
    protected GitSCMSource source;

    @Mock
    protected SCMRevisionImpl currRevision;

    @Mock
    protected SCMRevisionImpl lastBuiltRevision;

    @Mock
    protected SCMRevisionImpl lastSeenRevision;

    @Mock
    protected TaskListener listener;

    @Test
    public void should_return_TRUE_when_exceptionOccurred() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        given(source.getOwner()).willThrow(RuntimeException.class);

        // when
        boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

        // then
        assertTrue(result);
        assertFalse(buildStrategy.isShouldRunBuildExecuted);
    }

    @Test
    public void should_return_TRUE_when_failsToGetOwner() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        given(source.getOwner()).willReturn(null);

        // when
        boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

        // then
        assertTrue(result);
        assertFalse(buildStrategy.isShouldRunBuildExecuted);
    }

    @Test
    public void should_return_TRUE_when_failsToGetFileSystem() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        SCM scm = mock(SCM.class);
        given(source.build(head, currRevision)).willReturn(scm);

        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        given(source.getOwner()).willReturn(owner);

        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            mockedHelper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(null);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    public void should_return_TRUE_when_patternsEmptyAndStrategyExcluded() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        given(source.getOwner()).willReturn(owner);

        SCM scm = mock(SCM.class);
        given(source.build(head, currRevision)).willReturn(scm);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            mockedHelper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    public void should_return_FALSE_when_patternsEmptyAndStrategyIncluded() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(INCLUDED, patterns, expressions);

        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        given(source.getOwner()).willReturn(owner);

        SCM scm = mock(SCM.class);
        given(source.build(head, currRevision)).willReturn(scm);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            mockedHelper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertFalse(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    public void should_checkExpressionsAgainstPatterns() {
        // given
        Set<String> includedRegions = Sets.newHashSet("**/*.java", "src/main/resource/**/*.*");
        Set<String> paths = Sets.newHashSet("src/main/java/com/a/a.java", "src/main/java/com/a/b.java", "README.md");
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(INCLUDED, includedRegions, paths);

        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        given(source.getOwner()).willReturn(owner);

        SCM scm = mock(SCM.class);
        given(source.build(head, currRevision)).willReturn(scm);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            mockedHelper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    public void should_return_FALSE_when_patternsPresentAndStrategyExcludedAndlastBuiltRevisionNull() {
        // given
        Set<String> excludedRegions = Sets.newHashSet("*.md");
        Set<String> paths = Sets.newHashSet("README.md");
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, excludedRegions, paths);

        SCM scm = mock(SCM.class);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        given(source.getOwner()).willReturn(owner);

        SCMRevision lastBuiltRevisionNull = null;
        ChangeRequestSCMRevision currRevision = mock(ChangeRequestSCMRevision.class);

        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            mockedHelper.when(() -> BranchBuildStrategyHelper.buildSCMFileSystem(source, head, currRevision, scm, owner)).thenReturn(fileSystem);

            // when
            buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevisionNull, lastSeenRevision, listener);

            // then
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }
}
