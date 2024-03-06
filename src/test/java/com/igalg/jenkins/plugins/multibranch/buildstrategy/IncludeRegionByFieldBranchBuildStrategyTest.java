package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.then;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jenkins.scm.api.SCMFileSystem;

@RunWith(MockitoJUnitRunner.class)
public class IncludeRegionByFieldBranchBuildStrategyTest {

    @Mock
    private SCMFileSystem fileSystem;

    @Test
    public void should_return_setOfPatternsFromIncludedRegionText() {
        // given
        String line1 = "/path/foo/bar";
        String line2 = ""; // empty
        String line3 = "#some comment";
        String includedRegions = String.join("\n", line1, line2, line3);

        // when
        IncludeRegionBranchBuildStrategy branchBuildStrategy = new IncludeRegionByFieldBranchBuildStrategy(includedRegions);
        Set<String> patterns = branchBuildStrategy.getPatterns(fileSystem);

        // then
        assertEquals(1, patterns.size());
        assertEquals("path/foo/bar", patterns.iterator().next());
        then(fileSystem).shouldHaveNoInteractions();
    }

    @Test
    public void should_return_true_when_atLeastOnePathMatchesPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add("/path/foo/**");

        Set<String> paths = new HashSet<>();
        paths.add("/path/bar/something.java");
        paths.add("/path/foo/something.java");

        // when
        IncludeRegionBranchBuildStrategy branchBuildStrategy = new IncludeRegionByFieldBranchBuildStrategy("");
        boolean result = branchBuildStrategy.shouldRunBuild(patterns, paths);

        // then
        assertTrue(result);
    }

    @Test
    public void should_return_false_when_NonePathMatchPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add("/path/foo/**");

        Set<String> paths = new HashSet<>();
        paths.add("/path/bar/something.java");
        paths.add("/path/boo/something.java");

        // when
        IncludeRegionBranchBuildStrategy branchBuildStrategy = new IncludeRegionByFieldBranchBuildStrategy("");
        boolean result = branchBuildStrategy.shouldRunBuild(patterns, paths);

        // then
        assertFalse(result);
    }
}
