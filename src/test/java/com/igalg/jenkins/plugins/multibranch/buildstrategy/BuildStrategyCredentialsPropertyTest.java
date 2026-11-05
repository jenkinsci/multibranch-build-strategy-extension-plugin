package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;

class BuildStrategyCredentialsPropertyTest {

    private static GitSCM gitScm(String... urls) {
        GitSCM scm = mock(GitSCM.class);
        List<UserRemoteConfig> remotes = Arrays.stream(urls)
                .map(url -> new UserRemoteConfig(url, "origin", null, null))
                .toList();
        given(scm.getUserRemoteConfigs()).willReturn(remotes);
        return scm;
    }

    @Test
    void should_trimAndNormalise_when_constructed() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("  my-creds ", " BitBucket.org ");

        assertEquals("my-creds", property.getCredentialsId());
        assertEquals("bitbucket.org", property.getHosts());
    }

    @Test
    void should_returnNull_when_valuesBlank() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("   ", "");

        assertNull(property.getCredentialsId());
        assertNull(property.getHosts());
    }

    @Test
    void should_parseHostList_when_separatedByCommasOrSpaces() {
        BuildStrategyCredentialsProperty property =
                new BuildStrategyCredentialsProperty("creds", " BitBucket.org,ssh.bitbucket.org  bitbucket.org ,, ");

        assertEquals("bitbucket.org, ssh.bitbucket.org", property.getHosts());
        assertEquals(List.of("bitbucket.org", "ssh.bitbucket.org"), List.copyOf(property.getHostSet()));
    }

    @Test
    void should_extractHost_when_urlParsed() {
        assertEquals("bitbucket.org", BuildStrategyCredentialsProperty.hostOf("https://bitbucket.org/workspace/repo.git"));
        assertEquals("bitbucket.org", BuildStrategyCredentialsProperty.hostOf("https://user@BITBUCKET.org:443/workspace/repo.git"));
        assertEquals("bitbucket.org", BuildStrategyCredentialsProperty.hostOf("ssh://git@bitbucket.org:7999/workspace/repo.git"));
        assertEquals("bitbucket.org", BuildStrategyCredentialsProperty.hostOf("git@bitbucket.org:workspace/repo.git"));
    }

    @Test
    void should_returnNullHost_when_urlBlankOrLocal() {
        assertNull(BuildStrategyCredentialsProperty.hostOf(null));
        assertNull(BuildStrategyCredentialsProperty.hostOf(" "));
        assertNull(BuildStrategyCredentialsProperty.hostOf("/var/lib/git/repo.git"));
    }

    @Test
    void should_apply_when_allRemotesOnConfiguredHost() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("creds", "bitbucket.org");

        assertTrue(property.appliesTo(gitScm("https://bitbucket.org/ws/repo.git")));
        assertTrue(property.appliesTo(gitScm("https://bitbucket.org/ws/repo.git", "git@bitbucket.org:ws/fork.git")));
    }

    @Test
    void should_notApply_when_anyRemoteOnOtherHost() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("creds", "bitbucket.org");

        assertFalse(property.appliesTo(gitScm("https://attacker.example/ws/repo.git")));
        assertFalse(property.appliesTo(gitScm("https://bitbucket.org/ws/repo.git", "https://attacker.example/ws/repo.git")));
        assertFalse(property.appliesTo(gitScm("https://bitbucket.org.attacker.example/ws/repo.git")));
    }

    @Test
    void should_apply_when_remotesSpreadAcrossListedHosts() {
        BuildStrategyCredentialsProperty property =
                new BuildStrategyCredentialsProperty("creds", "bitbucket.org, ssh.bitbucket.org");

        assertTrue(property.appliesTo(gitScm("git@ssh.bitbucket.org:ws/repo.git")));
        assertTrue(property.appliesTo(gitScm("https://bitbucket.org/ws/repo.git", "git@ssh.bitbucket.org:ws/fork.git")));
        assertFalse(property.appliesTo(gitScm("git@ssh.bitbucket.org:ws/repo.git", "https://attacker.example/repo.git")));
    }

    @Test
    void should_notApply_when_sshHostNotListed() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("creds", "bitbucket.org");

        assertFalse(property.appliesTo(gitScm("git@ssh.bitbucket.org:ws/repo.git")));
    }

    @Test
    void should_notApply_when_scmNotGitOrWithoutRemotes() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("creds", "bitbucket.org");
        GitSCM noRemotes = mock(GitSCM.class);
        given(noRemotes.getUserRemoteConfigs()).willReturn(Collections.emptyList());

        assertFalse(property.appliesTo(null));
        assertFalse(property.appliesTo(mock(SCM.class)));
        assertFalse(property.appliesTo(noRemotes));
    }

    @Test
    void should_notApply_when_hostBlank() {
        BuildStrategyCredentialsProperty property = new BuildStrategyCredentialsProperty("creds", null);

        assertFalse(property.appliesTo(gitScm("https://bitbucket.org/ws/repo.git")));
    }
}
