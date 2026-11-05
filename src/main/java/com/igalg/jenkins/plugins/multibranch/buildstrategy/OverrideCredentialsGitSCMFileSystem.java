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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.GitTool;
import hudson.plugins.git.UserRemoteConfig;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.SCMHead;

/**
 * A {@link GitSCMFileSystem} fetched with an explicitly supplied credential.
 *
 * <p>{@code GitSCMFileSystem.BuilderImpl} offers two entry points and neither fits a credential override:
 * the {@code (SCMSource, SCMHead, SCMRevision)} overload always uses the source's own credential, and the
 * {@code (Item, SCM, SCMRevision)} overload ignores the SCM's refspecs and fetches
 * {@code refs/heads/<head name>}, which does not exist for change requests such as {@code PR-5}.
 * This builder fetches exactly the refspecs the SCM source configured (for example
 * {@code +refs/pull-requests/5/from:refs/remotes/origin/PR-5}), so branches and change requests both work.
 *
 * <p>Fetches go to a cache directory owned by this plugin, separate from the git plugin's own cache.
 */
final class OverrideCredentialsGitSCMFileSystem extends GitSCMFileSystem {

    private static final Logger LOGGER = Logger.getLogger(OverrideCredentialsGitSCMFileSystem.class.getName());

    private static final String CACHE_PREFIX = "multibranch-build-strategy-";

    private static final ConcurrentMap<String, Lock> CACHE_LOCKS = new ConcurrentHashMap<>();

    private OverrideCredentialsGitSCMFileSystem(GitClient client, String remote, String head,
                                                @CheckForNull AbstractGitSCMSource.SCMRevisionImpl rev)
            throws IOException, InterruptedException {
        super(client, remote, head, rev);
    }

    /**
     * Fetch every remote of {@code scm} with its configured refspecs, authenticating with {@code credential}.
     *
     * @return the file system, or {@code null} when {@code scm} has no usable remote
     */
    @CheckForNull
    static GitSCMFileSystem build(@NonNull GitSCM scm, @NonNull SCMHead head,
                                  @CheckForNull AbstractGitSCMSource.SCMRevisionImpl rev,
                                  @NonNull StandardUsernameCredentials credential)
            throws IOException, InterruptedException {
        List<UserRemoteConfig> userRemoteConfigs = scm.getUserRemoteConfigs();
        List<RemoteConfig> remoteConfigs = scm.getRepositories();
        String primaryRemote = userRemoteConfigs.isEmpty() ? null : userRemoteConfigs.get(0).getUrl();
        if (primaryRemote == null || remoteConfigs.isEmpty()) {
            LOGGER.warning("Git SCM has no remote URL");
            return null;
        }
        String primaryRemoteName = remoteConfigs.get(0).getName();

        String cacheEntry = CACHE_PREFIX + Util.getDigestOf(primaryRemote);
        Lock cacheLock = CACHE_LOCKS.computeIfAbsent(cacheEntry, k -> new ReentrantLock());
        TaskListener listener = new LogTaskListener(LOGGER, Level.FINE);

        cacheLock.lock();
        try {
            File cacheDir = new File(new File(Jenkins.get().getRootDir(), "caches"), cacheEntry);
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
                throw new IOException("Failed to create cache directory " + cacheDir);
            }
            Git git = Git.with(listener, new EnvVars(EnvVars.masterEnvVars)).in(cacheDir);
            GitTool tool = scm.resolveGitTool(listener);
            if (tool != null) {
                git.using(tool.getGitExe());
            }
            GitClient client = git.getClient();
            client.addDefaultCredentials(credential);
            if (!client.hasGitRepo(false)) {
                client.init();
            }

            for (RemoteConfig remoteConfig : remoteConfigs) {
                fetch(client, remoteConfig);
            }

            String headRef = Constants.R_REMOTES + primaryRemoteName + "/" + head.getName();
            return new OverrideCredentialsGitSCMFileSystem(client, primaryRemote, headRef, rev);
        } catch (GitException e) {
            throw new IOException(e);
        } finally {
            cacheLock.unlock();
        }
    }

    private static void fetch(GitClient client, RemoteConfig remoteConfig) throws InterruptedException, IOException {
        if (remoteConfig.getURIs().isEmpty()) {
            return;
        }
        String remoteName = remoteConfig.getName();
        List<RefSpec> refSpecs = remoteConfig.getFetchRefSpecs();
        client.setRemoteUrl(remoteName, remoteConfig.getURIs().get(0).toString());
        try {
            client.fetch_().prune(true).from(new URIish(remoteName), refSpecs).execute();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid remote name " + remoteName, e);
        }
    }
}
