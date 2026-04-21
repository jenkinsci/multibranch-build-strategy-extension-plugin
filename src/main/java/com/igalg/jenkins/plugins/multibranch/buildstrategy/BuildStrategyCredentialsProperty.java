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

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Project-scoped override for the credential used by branch build strategies
 * when they perform Git protocol operations (fetch, log) during branch scan.
 *
 * <p>The underlying strategies always talk Git, not REST. When the SCMSource
 * credential is a REST-only API token (e.g. Atlassian API scoped token with an
 * email username), Git HTTPS auth fails. Setting this property lets those
 * strategies use a separate, Git-compatible credential without changing the
 * SCMSource credential used for branch discovery.
 */
public class BuildStrategyCredentialsProperty extends AbstractFolderProperty<AbstractFolder<?>> {

    private final String credentialsId;

    @DataBoundConstructor
    public BuildStrategyCredentialsProperty(String credentialsId) {
        this.credentialsId = StringUtils.trimToNull(credentialsId);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Build strategy Git credential override";
        }

        @POST
        @SuppressWarnings("unused") // called by Jelly <c:select/>
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
            return result
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM2, context, StandardUsernameCredentials.class)
                    .includeCurrentValue(credentialsId);
        }

        @POST
        @SuppressWarnings("unused") // called by Jelly form validation
        public FormValidation doCheckCredentialsId(@AncestorInPath Item context,
                                                   @QueryParameter String value) {
            final String cleanedValue = StringUtils.trimToNull(value);
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            if (cleanedValue == null) {
                // empty is valid: disables the override, falls back to SCMSource credential
                return FormValidation.ok();
            }
            for (ListBoxModel.Option o : CredentialsProvider.listCredentialsInItem(
                    StandardUsernameCredentials.class,
                    context,
                    ACL.SYSTEM2,
                    Collections.emptyList(),
                    CredentialsMatchers.always())) {
                if (cleanedValue.equals(o.value)) {
                    return FormValidation.ok();
                }
            }
            return FormValidation.error("Cannot find any credentials with id " + cleanedValue);
        }
    }
}
