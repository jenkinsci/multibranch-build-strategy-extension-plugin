package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jenkins.scm.api.SCMFileSystem;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ExcludeMessageBranchBuildStrategyTest {

    @Mock
    private SCMFileSystem fileSystem;

    @Test
    void should_return_setOfPatternsFromExcludedMessagesText() {
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
    void should_return_TRUE_when_messageDoesNotMatchPattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");
        patterns.add(".*\\[maven\\-release\\-plugin\\].*");

        Set<String> singletonMessages = Collections.singleton("Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(result);
    }

    @Test
    void should_return_FALSE_when_messageMatchAtLeastOnePattern() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");
        patterns.add(".*\\[maven\\-release\\-plugin\\].*");

        Set<String> singletonMessages1 = Collections.singleton("[ci-skip] Example commit message");
        Set<String> singletonMessages2 = Collections.singleton("[maven-release-plugin] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result1 = buildStrategy.shouldRunBuild(patterns, singletonMessages1, false);
        boolean result2 = buildStrategy.shouldRunBuild(patterns, singletonMessages2, false);

        // then
        assertFalse(result1);
        assertFalse(result2);
    }

    @Test
    void should_return_TRUE_when_applyToChangeRequestsIsFalse_and_isChangeRequest() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(false);
        buildStrategy.setApplyToBranches(true);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);

        // then
        assertTrue(result, "Should not block change request when applyToChangeRequests is false");
    }

    @Test
    void should_return_FALSE_when_applyToChangeRequestsIsTrue_and_isChangeRequest() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);

        // then
        assertFalse(result, "Should block change request when applyToChangeRequests is true");
    }

    @Test
    void should_return_TRUE_when_applyToBranchesIsFalse_and_isNotChangeRequest() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(true);
        buildStrategy.setApplyToBranches(false);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(result, "Should not block branch build when applyToBranches is false");
    }

    @Test
    void should_return_FALSE_when_applyToBranchesIsTrue_and_isNotChangeRequest() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertFalse(result, "Should block branch build when applyToBranches is true");
    }

    @Test
    void should_return_TRUE_when_bothApplyFlagsAreFalse() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(false);
        buildStrategy.setApplyToBranches(false);
        boolean resultForPR = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);
        boolean resultForBranch = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(resultForPR, "Should not block PR when both flags are false");
        assertTrue(resultForBranch, "Should not block branch when both flags are false");
    }

    @Test
    void should_return_TRUE_when_applyToChangeRequestsIsFalse_and_isChangeRequest_and_messageDoesNotMatch() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("Normal commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(false);
        buildStrategy.setApplyToBranches(true);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);

        // then
        assertTrue(result, "Should allow change request when applyToChangeRequests is false, regardless of message");
    }

    @Test
    void should_return_TRUE_when_applyToBranchesIsFalse_and_isNotChangeRequest_and_messageDoesNotMatch() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("Normal commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(true);
        buildStrategy.setApplyToBranches(false);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(result, "Should allow branch build when applyToBranches is false, regardless of message");
    }

    @Test
    void should_return_TRUE_when_applyToChangeRequestsIsTrue_and_isChangeRequest_and_messageDoesNotMatch() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("Normal commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);

        // then
        assertTrue(result, "Should allow change request when message doesn't match pattern");
    }

    @Test
    void should_return_TRUE_when_applyToBranchesIsTrue_and_isNotChangeRequest_and_messageDoesNotMatch() {
        // given
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("Normal commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(result, "Should allow branch build when message doesn't match pattern");
    }

    @Test
    void should_return_TRUE_when_applyToChangeRequestsIsTrue_applyToBranchesIsFalse_and_isNotChangeRequest_and_messageMatches() {
        // given - configured to block PRs but not branches
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(true);
        buildStrategy.setApplyToBranches(false);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, false);

        // then
        assertTrue(result, "Should allow branch build even when message matches, because applyToBranches is false");
    }

    @Test
    void should_return_TRUE_when_applyToChangeRequestsIsFalse_applyToBranchesIsTrue_and_isChangeRequest_and_messageMatches() {
        // given - configured to block branches but not PRs
        Set<String> patterns = new HashSet<>();
        patterns.add(".*\\[ci\\-skip\\].*");

        Set<String> singletonMessages = Collections.singleton("[ci-skip] Example commit message");

        // when
        ExcludeMessageBranchBuildStrategy buildStrategy = new ExcludeMessageBranchBuildStrategy("");
        buildStrategy.setApplyToChangeRequests(false);
        buildStrategy.setApplyToBranches(true);
        boolean result = buildStrategy.shouldRunBuild(patterns, singletonMessages, true);

        // then
        assertTrue(result, "Should allow change request even when message matches, because applyToChangeRequests is false");
    }
}

// Made with Bob
