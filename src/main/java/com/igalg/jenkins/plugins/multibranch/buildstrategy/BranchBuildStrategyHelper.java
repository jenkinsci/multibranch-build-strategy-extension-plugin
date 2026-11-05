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

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.gitclient.GitClient;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Item;
import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.security.ACL;
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

    /**
     * Build the file system used to read pattern files and compute change sets.
     *
     * @param credentialsIdOverride credential to authenticate the Git fetch with instead of the SCM source's own
     *                              credential, or {@code null} to keep the SCM source's behaviour unchanged. Callers
     *                              must only pass an override that is allowed for every remote of {@code scm}.
     */
    static SCMFileSystem buildSCMFileSystem(SCMSource source, SCMHead head, SCMRevision currRevision, SCM scm, SCMSourceOwner owner, @CheckForNull String credentialsIdOverride) throws IOException, InterruptedException {
        final SCMRevision revision = toGitRevision(head, currRevision);

        if (StringUtils.isNotBlank(credentialsIdOverride) && scm instanceof GitSCM gitSCM) {
            final StandardUsernameCredentials credential = lookupGitCredential(credentialsIdOverride, owner, gitSCM);
            if (credential != null) {
                return OverrideCredentialsGitSCMFileSystem.build(gitSCM, head, (AbstractGitSCMSource.SCMRevisionImpl) revision, credential);
            }
            LOGGER.warning(() -> String.format("Credential override '%s' not found or not usable for Git in %s; "
                    + "falling back to the SCM source credential", credentialsIdOverride, owner.getFullName()));
        }

        final SCMFileSystem.Builder builder = new GitSCMFileSystem.BuilderImpl();
        if (revision != currRevision) {
            return builder.build(source, head, revision);
        }
        return builder.build(owner, scm, currRevision);
    }

    /**
     * Look the credential up the same way the git plugin does when it fetches: in the context of the owner,
     * restricted to the remote URI and to credential types Git can use.
     */
    @CheckForNull
    static StandardUsernameCredentials lookupGitCredential(String credentialsId, Item owner, GitSCM scm) {
        StandardUsernameCredentials credential = null;
        for (UserRemoteConfig remote : scm.getUserRemoteConfigs()) {
            StandardUsernameCredentials found = CredentialsProvider.findCredentialByIdInItem(
                    credentialsId,
                    StandardUsernameCredentials.class,
                    owner,
                    ACL.SYSTEM2,
                    URIRequirementBuilder.fromUri(remote.getUrl()).build());
            if (found == null || !GitClient.CREDENTIALS_MATCHER.matches(found)) {
                return null;
            }
            credential = found;
        }
        if (credential != null) {
            CredentialsProvider.track(owner, credential);
        }
        return credential;
    }

    /**
     * Git file systems only understand {@link AbstractGitSCMSource.SCMRevisionImpl}. Other revisions (for example
     * change request revisions) are converted using the leading commit hash of their string form.
     */
    static SCMRevision toGitRevision(SCMHead head, SCMRevision revision) {
        if (revision == null || revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            return revision;
        }
        return new AbstractGitSCMSource.SCMRevisionImpl(head, revision.toString().substring(0, HASH_LENGTH));
    }

    static List<GitChangeSet> getGitChangeSetList(SCMFileSystem fileSystem, SCMHead head, SCMRevision revision) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fileSystem.changesSince(toGitRevision(head, revision), out);
        GitChangeLogParser parser = new GitChangeLogParser(null, false);
        return parser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Reads the patterns from a file at the root of the SCM.
     * <p>
     * A missing file (or a path that is not a file) yields an empty set. Any failure while looking up or reading the
     * file is propagated, so that {@code AbstractBranchBuildStrategy#isAutomaticBuild} can fail open instead of
     * mistaking an infrastructure error for "no patterns".
     */
    static Set<String> getPatternsFromFile(SCMFileSystem fileSystem, String filePath)
            throws IOException, InterruptedException {
        LOGGER.info(() -> String.format("Looking for file: %s", filePath));

        final SCMFile ignorefile = fileSystem.getRoot().child(filePath);
        if (!ignorefile.exists() || !ignorefile.isFile()) {
            LOGGER.severe(() -> String.format("File: %s not found", filePath));
            return Collections.emptySet();
        }

        return toPatterns(ignorefile.contentAsString());
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
