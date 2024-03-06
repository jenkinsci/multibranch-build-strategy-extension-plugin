package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jenkins.scm.api.SCMFileSystem;
import org.junit.Test;
import org.mockito.Mock;

public class ExcludeMessageBranchBuildStrategyTest {

    @Mock
    private SCMFileSystem fileSystem;

    @Test
    public void should_return_setOfPatternsFromExcludedMessagesText() {
        // given
        String line1 = ".*\\[ci\\-skip\\].*";
        String line2 = ".*\\[maven\\-release\\-plugin\\].*";
        String line3 = ""; // empty line
        String excludedMessages = String.join("\n", line1, line2, line3);

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy(excludedMessages);
        Set<String> patterns = buildStrategy.getPatterns(fileSystem);
        Iterator<String> iterator = patterns.iterator();

        // then
        assertEquals(2, patterns.size());
        assertEquals(".*\\[ci\\-skip\\].*", iterator.next());
        assertEquals(".*\\[maven\\-release\\-plugin\\].*", iterator.next());
    }

    @Test
    public void should_return_TRUE_when_messageDoesNotMatchPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");
        patterns.add(".*\\[maven\\-release\\-plugin\\].*");

        Set<String> singletonMessages = Collections.singleton("Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages);

        // then
        assertTrue(result);
    }

    @Test
    public void should_return_FALSE_when_messageMatchAtLeastOnePattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");
        patterns.add(".*\\[maven\\-release\\-plugin\\].*");

        Set<String> singletonMessages1 = Collections.singleton("[ci-skip] Example commit message");
        Set<String> singletonMessages2 = Collections.singleton("[maven-release-plugin] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result1 = buildStrategy.shouldRunBuild(patterns, singletonMessages1);
        boolean result2 = buildStrategy.shouldRunBuild(patterns, singletonMessages2);

        // then
        assertFalse(result1);
        assertFalse(result2);
    }
}
