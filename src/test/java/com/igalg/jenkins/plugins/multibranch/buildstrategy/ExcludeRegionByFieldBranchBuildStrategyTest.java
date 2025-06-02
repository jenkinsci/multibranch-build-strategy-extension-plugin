package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.then;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import jenkins.scm.api.SCMFileSystem;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcludeRegionByFieldBranchBuildStrategyTest {

    @Mock
    private SCMFileSystem fileSystem;

    @Test
    void should_return_setOfPatternsFromExcludedRegionText() {
        // given
        String line1 = "/path/foo/bar";
        String line2 = ""; // empty
        String line3 = "#some comment";
        String excludedRegions = String.join("\n", line1, line2, line3);

        // when
        ExcludeRegionBranchBuildStrategy branchBuildStrategy = new ExcludeRegionByFieldBranchBuildStrategy(excludedRegions);
        Set<String> patterns = branchBuildStrategy.getPatterns(fileSystem);

        // then
        assertEquals(1, patterns.size());
        assertEquals("path/foo/bar", patterns.iterator().next());
        then(fileSystem).shouldHaveNoInteractions();
    }

    @Test
    void should_return_true_when_atLeastOnePathDoesNotMatchPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add("/foo/**");
        patterns.add("/path/foo/**");

        Set<String> paths = new HashSet<>();
        paths.add("/path/bar/something.java");
        paths.add("/path/foo/something.java");

        // when
        ExcludeRegionBranchBuildStrategy branchBuildStrategy = new ExcludeRegionByFieldBranchBuildStrategy("");
        boolean result = branchBuildStrategy.shouldRunBuild(patterns, paths);

        // then
        assertTrue(result);
    }

    @Test
    void should_return_false_when_AllPathsMatchPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add("/foo/**");
        patterns.add("/path/foo/**");

        Set<String> paths = new HashSet<>();
        paths.add("/path/foo/something.java");
        paths.add("/path/foo/bar/something.java");
        paths.add("/foo/something.java");

        // when
        ExcludeRegionBranchBuildStrategy branchBuildStrategy = new ExcludeRegionByFieldBranchBuildStrategy("");
        boolean result = branchBuildStrategy.shouldRunBuild(patterns, paths);

        // then
        assertFalse(result);
    }
}
