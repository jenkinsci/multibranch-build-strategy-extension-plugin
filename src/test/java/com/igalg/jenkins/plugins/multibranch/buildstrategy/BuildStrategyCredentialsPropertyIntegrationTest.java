package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.Item;
import hudson.model.User;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

@WithJenkins
class BuildStrategyCredentialsPropertyIntegrationTest {

    private static final String HOST = "bitbucket.org";

    private static final String REMOTE = "https://bitbucket.org/workspace/repo.git";

    private JenkinsRule jenkins;

    private BuildStrategyCredentialsProperty.DescriptorImpl descriptor;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        descriptor = jenkins.jenkins.getDescriptorByType(BuildStrategyCredentialsProperty.DescriptorImpl.class);

        SystemCredentialsProvider provider = SystemCredentialsProvider.getInstance();
        provider.getCredentials().add(new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "git-creds", "global", "git-user", "git-password-1234"));
        provider.getDomainCredentialsMap().put(
                new Domain("other", "other host only", List.of(new HostnameSpecification("other.example", null))),
                List.of(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL, "other-host-creds", "other host", "other-user", "other-password-1234")));
        provider.save();
    }

    private static GitSCM gitScm(String url) {
        return new GitSCM(
                List.of(new UserRemoteConfig(url, "origin", null, null)),
                List.of(new BranchSpec("*/master")),
                null, null, Collections.emptyList());
    }

    @Test
    void should_resolveOverride_when_setOnProjectForMatchingHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        project.getProperties().add(new BuildStrategyCredentialsProperty("git-creds", HOST));

        assertEquals("git-creds", AbstractBranchBuildStrategy.resolveCredentialsOverride(project, gitScm(REMOTE)));
    }

    @Test
    void should_notResolveOverride_when_remoteOnOtherHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        project.getProperties().add(new BuildStrategyCredentialsProperty("git-creds", HOST));

        assertNull(AbstractBranchBuildStrategy.resolveCredentialsOverride(project, gitScm("https://attacker.example/repo.git")));
    }

    @Test
    void should_notInheritOverride_when_setOnEnclosingFolder() throws Exception {
        Folder folder = jenkins.createProject(Folder.class, "folder");
        folder.getProperties().add(new BuildStrategyCredentialsProperty("git-creds", HOST));
        WorkflowMultiBranchProject project = folder.createProject(WorkflowMultiBranchProject.class, "project");

        assertNull(BuildStrategyCredentialsProperty.forOwner(project));
        assertNull(AbstractBranchBuildStrategy.resolveCredentialsOverride(project, gitScm(REMOTE)));
    }

    @Test
    void should_disableOverride_when_credentialBlank() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        project.getProperties().add(new BuildStrategyCredentialsProperty("", HOST));

        assertNull(BuildStrategyCredentialsProperty.forOwner(project));
    }

    @Test
    void should_disableOverride_when_whitespaceLoadedFromConfigXml() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        BuildStrategyCredentialsProperty property = (BuildStrategyCredentialsProperty) Jenkins.XSTREAM2.fromXML(
                "<" + BuildStrategyCredentialsProperty.class.getName() + ">"
                        + "<credentialsId>   </credentialsId><hosts>" + HOST + "</hosts>"
                        + "</" + BuildStrategyCredentialsProperty.class.getName() + ">");
        project.getProperties().add(property);

        assertNull(property.getCredentialsId());
        assertNull(BuildStrategyCredentialsProperty.forOwner(project));
    }

    @Test
    void should_keepValues_when_configRoundTripped() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        project.getProperties().add(new BuildStrategyCredentialsProperty("git-creds", HOST));

        jenkins.configRoundtrip(project);

        BuildStrategyCredentialsProperty property = project.getProperties().get(BuildStrategyCredentialsProperty.class);
        assertNotNull(property);
        assertEquals("git-creds", property.getCredentialsId());
        assertEquals(HOST, property.getHosts());
    }

    @Test
    void should_onlyBeApplicable_toMultiBranchProjects() {
        assertTrue(descriptor.isApplicable(WorkflowMultiBranchProject.class));
        assertFalse(descriptor.isApplicable(Folder.class));
    }

    @Test
    void should_listOnlyCredentialsUsableForHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");

        Set<String> ids = ids(descriptor.doFillCredentialsIdItems(project, HOST, null));

        assertTrue(ids.contains("git-creds"));
        assertFalse(ids.contains("other-host-creds"));
    }

    @Test
    void should_onlyOfferCredentialsUsableForEveryListedHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");

        Set<String> ids = ids(descriptor.doFillCredentialsIdItems(project, "other.example, ssh.bitbucket.org", null));

        assertTrue(ids.contains("git-creds"));
        assertFalse(ids.contains("other-host-creds"));
        assertEquals(FormValidation.Kind.ERROR,
                descriptor.doCheckCredentialsId(project, "other.example, ssh.bitbucket.org", "other-host-creds").kind);
        assertEquals(FormValidation.Kind.OK,
                descriptor.doCheckCredentialsId(project, "other.example", "other-host-creds").kind);
    }

    @Test
    void should_resolveOverride_when_sshRemoteOnSecondListedHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        project.getProperties().add(new BuildStrategyCredentialsProperty("git-creds", "bitbucket.org, ssh.bitbucket.org"));

        assertEquals("git-creds",
                AbstractBranchBuildStrategy.resolveCredentialsOverride(project, gitScm("git@ssh.bitbucket.org:workspace/repo.git")));
    }

    @Test
    void should_validateCredentialAgainstHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");

        assertEquals(FormValidation.Kind.OK, descriptor.doCheckCredentialsId(project, HOST, "git-creds").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckCredentialsId(project, HOST, " ").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCredentialsId(project, HOST, "other-host-creds").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCredentialsId(project, HOST, "missing").kind);
    }

    @Test
    void should_validateHost() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");

        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHosts(project, "git-creds", "BitBucket.org").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHosts(project, "", "").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckHosts(project, "git-creds", "").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckHosts(project, "git-creds", "https://bitbucket.org/").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckHosts(project, "git-creds", "bitbucket.org:443").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHosts(project, "git-creds", "bitbucket.org, ssh.bitbucket.org").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckHosts(project, "git-creds", "bitbucket.org, https://x").kind);
    }

    @Test
    void should_notListCredentials_when_userLacksPermission() throws Exception {
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject.class, "project");
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ).everywhere().to("reader"));

        try (ACLContext ignored = ACL.as2(User.getById("reader", true).impersonate2())) {
            Set<String> ids = ids(descriptor.doFillCredentialsIdItems(project, HOST, "current"));
            assertEquals(Set.of("current"), ids);
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckCredentialsId(project, HOST, "missing").kind);
        }
    }

    private static Set<String> ids(ListBoxModel model) {
        return model.stream().map(option -> option.value).filter(v -> !v.isEmpty()).collect(Collectors.toSet());
    }
}
