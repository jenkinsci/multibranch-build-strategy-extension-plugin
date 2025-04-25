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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import com.google.common.collect.Sets;

import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeSet;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestSCMRevision;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractBranchBuildStrategyExtensionTest {

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
    void should_return_TRUE_when_exceptionOccurred() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        // when
        boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

        // then
        assertTrue(result);
        assertFalse(buildStrategy.isShouldRunBuildExecuted);
    }

    @Test
    void should_return_TRUE_when_failsToGetOwner() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        // when
        boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

        // then
        assertTrue(result);
        assertFalse(buildStrategy.isShouldRunBuildExecuted);
    }

    @Test
    void should_return_TRUE_when_failsToGetFileSystem() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        try (MockedStatic<SCMFileSystem> mockedFileSystem = mockStatic(SCMFileSystem.class)) {
            mockedFileSystem.when(() -> SCMFileSystem.of(source, head, currRevision)).thenReturn(null);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    void should_return_TRUE_when_patternsEmptyAndStrategyExcluded() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, expressions);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<SCMFileSystem> mockedFileSystem = mockStatic(SCMFileSystem.class)) {
            mockedFileSystem.when(() -> SCMFileSystem.of(source, head, currRevision)).thenReturn(fileSystem);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    void should_return_FALSE_when_patternsEmptyAndStrategyIncluded() {
        // given
        Set<String> patterns = Sets.newHashSet();
        Set<String> expressions = Sets.newHashSet();
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(INCLUDED, patterns, expressions);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<SCMFileSystem> mockedFileSystem = mockStatic(SCMFileSystem.class)) {
            mockedFileSystem.when(() -> SCMFileSystem.of(source, head, currRevision)).thenReturn(fileSystem);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertFalse(result);
            assertFalse(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    void should_checkExpressionsAgainstPatterns() {
        // given
        Set<String> includedRegions = Sets.newHashSet("**/*.java", "src/main/resource/**/*.*");
        Set<String> paths = Sets.newHashSet("src/main/java/com/a/a.java", "src/main/java/com/a/b.java", "README.md");
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(INCLUDED, includedRegions, paths);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        try (MockedStatic<SCMFileSystem> mockedFileSystem = mockStatic(SCMFileSystem.class)) {
            mockedFileSystem.when(() -> SCMFileSystem.of(source, head, currRevision)).thenReturn(fileSystem);

            // when
            buildStrategy.isAutomaticBuild(source, head, currRevision, lastBuiltRevision, lastSeenRevision, listener);

            // then
            assertTrue(buildStrategy.isShouldRunBuildExecuted);
        }
    }

    @Test
    void should_return_FALSE_when_patternsPresentAndStrategyExcludedAndLastBuiltRevisionNull() {
        // given
        Set<String> patterns = Sets.newHashSet("*.md");
        Set<String> paths = Sets.newHashSet("README.md");
        TestBranchBuildStrategy buildStrategy = new TestBranchBuildStrategy(EXCLUDED, patterns, paths);

        GitSCMFileSystem fileSystem = mock(GitSCMFileSystem.class);

        SCMRevision lastBuiltRevisionNull = null;
        ChangeRequestSCMRevision<?> prRevision = mock(ChangeRequestSCMRevision.class);

        try (MockedStatic<SCMFileSystem> mockedFileSystem = mockStatic(SCMFileSystem.class)) {
            mockedFileSystem.when(() -> SCMFileSystem.of(source, head, prRevision)).thenReturn(fileSystem);

            // when
            boolean result = buildStrategy.isAutomaticBuild(source, head, prRevision, lastBuiltRevisionNull, lastSeenRevision, listener);

            // then
            assertFalse(result);
        }
    }
}
