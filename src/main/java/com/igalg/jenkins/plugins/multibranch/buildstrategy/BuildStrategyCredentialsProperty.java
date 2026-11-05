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

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.branch.MultiBranchProject;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;

/**
 * Per-project override for the credential used by branch build strategies when they perform Git protocol
 * operations (fetch, log) during branch scan.
 *
 * <p>The strategies always talk Git, not REST. When the SCM source credential is a REST-only API token (for example
 * an Atlassian API scoped token with an email username), Git HTTPS auth fails. Setting this property lets the
 * strategies use a separate, Git-compatible credential without changing the credential used for branch discovery.
 *
 * <p>The property is only read from the multibranch project itself; it is never inherited from enclosing folders.
 * The credential is only sent when every remote of the branch's SCM is on one of the configured
 * {@linkplain #getHosts() hosts}, so changing the project's repository URL to another host cannot redirect the
 * credential there. Several hosts can be listed for providers that use separate hosts for HTTPS and SSH, for example
 * {@code bitbucket.org} and {@code ssh.bitbucket.org}.
 */
public class BuildStrategyCredentialsProperty extends AbstractFolderProperty<AbstractFolder<?>> {

    private static final Pattern HOST_PATTERN = Pattern.compile("[a-z0-9]([a-z0-9.-]*[a-z0-9])?");

    private static final Pattern HOST_SEPARATOR = Pattern.compile("[,\\s]+");

    private final String credentialsId;

    private final String hosts;

    @DataBoundConstructor
    public BuildStrategyCredentialsProperty(String credentialsId, String hosts) {
        this.credentialsId = StringUtils.trimToNull(credentialsId);
        this.hosts = StringUtils.trimToNull(String.join(", ", parseHosts(hosts)));
    }

    /**
     * @return the credential ID, or {@code null} when blank (including blank values loaded from {@code config.xml})
     */
    @CheckForNull
    public String getCredentialsId() {
        return StringUtils.trimToNull(credentialsId);
    }

    /**
     * @return the Git hosts the credential may be sent to, lower-cased and comma-separated, or {@code null} when blank
     */
    @CheckForNull
    public String getHosts() {
        return StringUtils.trimToNull(String.join(", ", getHostSet()));
    }

    /**
     * @return the Git hosts the credential may be sent to, lower-cased; empty when none are configured
     */
    @NonNull
    Set<String> getHostSet() {
        return parseHosts(hosts);
    }

    /**
     * Find the override configured on {@code owner} itself. Enclosing folders are deliberately not consulted.
     *
     * @return the property when it has both a credential and at least one host, otherwise {@code null}
     */
    @CheckForNull
    static BuildStrategyCredentialsProperty forOwner(@CheckForNull SCMSourceOwner owner) {
        if (!(owner instanceof AbstractFolder<?> folder)) {
            return null;
        }
        BuildStrategyCredentialsProperty property = folder.getProperties().get(BuildStrategyCredentialsProperty.class);
        if (property == null || property.getCredentialsId() == null || property.getHostSet().isEmpty()) {
            return null;
        }
        return property;
    }

    /**
     * @return {@code true} when {@code scm} is a Git SCM whose remotes are all on one of {@link #getHostSet()}
     */
    boolean appliesTo(@CheckForNull SCM scm) {
        Set<String> allowedHosts = getHostSet();
        if (allowedHosts.isEmpty() || !(scm instanceof GitSCM gitSCM)) {
            return false;
        }
        List<UserRemoteConfig> remotes = gitSCM.getUserRemoteConfigs();
        if (remotes.isEmpty()) {
            return false;
        }
        return remotes.stream().allMatch(remote -> {
            String remoteHost = hostOf(remote.getUrl());
            return remoteHost != null && allowedHosts.contains(remoteHost);
        });
    }

    @CheckForNull
    static String hostOf(@CheckForNull String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return normalizeHost(new URIish(url.trim()).getHost());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @CheckForNull
    static String normalizeHost(@CheckForNull String value) {
        String trimmed = StringUtils.trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * Split a comma- or whitespace-separated list of hosts, lower-casing each and dropping blanks and duplicates.
     */
    @NonNull
    static Set<String> parseHosts(@CheckForNull String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }
        return Arrays.stream(HOST_SEPARATOR.split(value.trim()))
                .map(BuildStrategyCredentialsProperty::normalizeHost)
                .filter(host -> host != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static List<DomainRequirement> domainRequirements(@CheckForNull String host) {
        String normalized = normalizeHost(host);
        if (normalized == null) {
            return Collections.emptyList();
        }
        return URIRequirementBuilder.create().withHostname(normalized).build();
    }

    /**
     * Git-usable credentials visible in {@code context} that may be used with every one of {@code hosts}, matching
     * the per-remote lookup done at scan time. With no hosts, no host restriction is applied.
     */
    static Map<String, String> usableCredentials(@CheckForNull Item context, @NonNull Set<String> hosts) {
        List<String> hostList = hosts.isEmpty() ? Collections.singletonList(null) : List.copyOf(hosts);
        Map<String, String> result = null;
        for (String host : hostList) {
            Map<String, String> forHost = new LinkedHashMap<>();
            for (ListBoxModel.Option option : CredentialsProvider.listCredentialsInItem(
                    StandardUsernameCredentials.class,
                    context,
                    ACL.SYSTEM2,
                    domainRequirements(host),
                    GitClient.CREDENTIALS_MATCHER)) {
                forHost.put(option.value, option.name);
            }
            if (result == null) {
                result = forHost;
            } else {
                result.keySet().retainAll(forHost.keySet());
            }
        }
        return result;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Build strategy Git credential override";
        }

        @Override
        @SuppressWarnings("rawtypes") // signature inherited from AbstractFolderPropertyDescriptor
        public boolean isApplicable(Class<? extends AbstractFolder> containerType) {
            return MultiBranchProject.class.isAssignableFrom(containerType);
        }

        @POST
        @SuppressWarnings("unused") // called by Jelly <c:select/>
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String hosts,
                                                     @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (!canListCredentials(context)) {
                return result.includeCurrentValue(credentialsId);
            }
            result.includeEmptyValue();
            usableCredentials(context, parseHosts(hosts)).forEach((id, name) -> result.add(name, id));
            return result.includeCurrentValue(credentialsId);
        }

        @POST
        @SuppressWarnings("unused") // called by Jelly form validation
        public FormValidation doCheckCredentialsId(@AncestorInPath Item context,
                                                   @QueryParameter String hosts,
                                                   @QueryParameter String value) {
            if (!canListCredentials(context)) {
                return FormValidation.ok();
            }
            String id = StringUtils.trimToNull(value);
            if (id == null) {
                return FormValidation.ok("No override: the SCM source credential is used.");
            }
            Set<String> hostSet = parseHosts(hosts);
            if (usableCredentials(context, hostSet).containsKey(id)) {
                return FormValidation.ok();
            }
            String forHosts = hostSet.isEmpty() ? "" : " for host(s) " + String.join(", ", hostSet);
            return FormValidation.error("Cannot find a Git username/password or SSH credential with id " + id + forHosts);
        }

        @POST
        @SuppressWarnings("unused") // called by Jelly form validation
        public FormValidation doCheckHosts(@AncestorInPath Item context,
                                           @QueryParameter String credentialsId,
                                           @QueryParameter String value) {
            if (!canListCredentials(context)) {
                return FormValidation.ok();
            }
            Set<String> hostSet = parseHosts(value);
            if (hostSet.isEmpty()) {
                return StringUtils.isBlank(credentialsId)
                        ? FormValidation.ok()
                        : FormValidation.error("Required: the credential is only sent to remotes on these hosts.");
            }
            for (String host : hostSet) {
                if (!HOST_PATTERN.matcher(host).matches()) {
                    return FormValidation.error("'" + host + "' is not a host name. Enter host names only, for example "
                            + "bitbucket.org, ssh.bitbucket.org (no scheme, port or path).");
                }
            }
            return FormValidation.ok();
        }

        private static boolean canListCredentials(@CheckForNull Item context) {
            if (context == null) {
                return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
            }
            return context.hasPermission(Item.EXTENDED_READ) || context.hasPermission(CredentialsProvider.USE_ITEM);
        }
    }
}
