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

import hudson.plugins.git.GitChangeSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractMessageBranchBuildStrategy extends AbstractBranchBuildStrategy {

    private static final Logger LOGGER = Logger.getLogger(AbstractMessageBranchBuildStrategy.class.getName());

    protected AbstractMessageBranchBuildStrategy() {
        super(Strategy.EXCLUDED);
    }

    @Override
    Set<String> getExpressions(List<GitChangeSet> changeSets) {
        GitChangeSet lastCommit = changeSets.get(0);
        final String commitMessage = lastCommit.getMsg();
        LOGGER.log(Level.FINE, () -> "Message: \"" + commitMessage + "\" from commit: " + lastCommit.getCommitId());

        return Collections.singleton(commitMessage);
    }

    @Override
    boolean shouldRunBuild(Set<String> patterns, Set<String> singletonMessages) {
        String message = singletonMessages.iterator().next();
        boolean isNotMatchingAnyPattern = true;

        for (String pattern : patterns) {
            if (matchPattern(pattern, message)) {
                LOGGER.log(Level.FINE, () -> "Matched excluded message pattern: " + pattern + " for message: \"" + message + "\"");
                isNotMatchingAnyPattern = false;
                break;
            } else {
                LOGGER.log(Level.FINE, () -> "Not matching excluded message pattern: " + pattern + " for message: \"" + message + "\"");
            }
        }

        if (isNotMatchingAnyPattern) {
            LOGGER.log(Level.INFO, () -> "Message: \"" + message + "\" does not match any excluded message pattern [" + String.join(", ", patterns) + "], build should be triggered");
            return true;
        }

        LOGGER.log(Level.INFO, "Commit message matching excluded message pattern, skipping build");
        return false;
    }

    private boolean matchPattern(String pattern, String message) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(message);

        return matcher.find();
    }
}
