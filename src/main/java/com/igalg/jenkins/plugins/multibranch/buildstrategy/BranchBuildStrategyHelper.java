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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

final class BranchBuildStrategyHelper {

    private static final Logger LOGGER = Logger.getLogger(BranchBuildStrategyHelper.class.getName());

    private static final int HASH_LENGTH = 40;

    private BranchBuildStrategyHelper() {
    }

    static SCMFileSystem buildSCMFileSystem(SCMSource source, SCMHead head, SCMRevision currRevision, SCM scm, SCMSourceOwner owner) throws IOException, InterruptedException {
        SCMFileSystem.Builder builder = new GitSCMFileSystem.BuilderImpl();
        if (currRevision != null && !(currRevision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
            return builder.build(source, head, new AbstractGitSCMSource.SCMRevisionImpl(head, currRevision.toString().substring(0, 40)));
        } else {
            return builder.build(owner, scm, currRevision);
        }
    }

    static List<GitChangeSet> getGitChangeSetList(SCMFileSystem fileSystem, SCMHead head, SCMRevision revision) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (revision != null && !(revision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
            fileSystem.changesSince(new AbstractGitSCMSource.SCMRevisionImpl(head, revision.toString().substring(0, HASH_LENGTH)), out);
        } else {
            fileSystem.changesSince(revision, out);
        }
        GitChangeLogParser parser = new GitChangeLogParser(null, false);
        return parser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    static Set<String> getPatternsFromFile(SCMFileSystem fileSystem, String filePath) {
        try {
            LOGGER.info(() -> String.format("Looking for file: %s", filePath));

            final SCMFile ignorefile = fileSystem.getRoot().child(filePath);
            if (!ignorefile.exists() || !ignorefile.isFile()) {
                LOGGER.severe(() -> String.format("File: %s not found", filePath));
                return Collections.emptySet();
            }

            return toPatterns(ignorefile.contentAsString());
        } catch (final Exception e) {
            LOGGER.severe("Unexpected exception: " + e);

            if (e instanceof InterruptedException) {
                // Clean up whatever needs to be handled before interrupting
                Thread.currentThread().interrupt();
            }

            // we don't want to cancel builds on unexpected exception
            return Collections.emptySet();
        }
    }

    static Set<String> toPatterns(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        return Arrays.stream(value.split("\n"))
                .filter(p -> !p.startsWith("#"))
                .filter(StringUtils::isNotBlank)
                .map(row -> {
                    // path from ChangeSet does not start with "/"
                    if (row.startsWith("/")) {
                        return row.substring(1);
                    }
                    return row;
                })
                .map(String::trim)
                .collect(Collectors.toSet());
    }

}
