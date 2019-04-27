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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSourceOwner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IncludeRegionBranchBuildStrategy.class, GitSCMSource.class})
public class RegionBranchBuildStrategyTest{
	
	
	private SCMHead head;
    private GitSCMSource source;
    private SCMRevisionImpl currRevision;
    private SCMRevisionImpl prevRevision;
    private List<String> includeRegions = Arrays.asList("**/*.java", "src/main/resource/**/*.*");
    private List<String> excludeRegions = Arrays.asList("*.md", "*.gitignore");
    private SCM scm;
    
    
    
    @Before
    public void setUp() {
        GitSCMSource sourceMock = PowerMockito.mock(GitSCMSource.class);
        this.head = new SCMHead("test-branch");
        this.source = sourceMock;                      
        this.currRevision = new SCMRevisionImpl(head, randomHash());
        this.prevRevision = new SCMRevisionImpl(head, randomHash());
        this.scm = new GitSCM("http://test.com");
    }
    
    
    
    

    
    
    @Test
    public void testIncludeIsReturnsTrueIfAtleastOneAffectedFileInTheRange() throws Exception {
        String commits = generateCommitLogString("src/main/java/com/a/a.java","src/main/java/com/a/b.java","README.md");
        setupGitSourceMocksWithCommits(commits);
        IncludeRegionBranchBuildStrategy includeRegionBranchBuildStrategy = new IncludeRegionBranchBuildStrategy(String.join("\n", includeRegions));
        assertTrue(includeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    
    @Test
    public void testIncludeIsReturnsTrueIfAtleastOneAffectedFileInTheSecondCommit() throws Exception {
        String commits = generateCommitLogString(".gitignore","README.md") + generateCommitLogString("src/main/java/com/a/a.java","src/main/java/com/a/b.java","README.md");
        setupGitSourceMocksWithCommits(commits);
        System.err.println(commits);
        IncludeRegionBranchBuildStrategy includeRegionBranchBuildStrategy = new IncludeRegionBranchBuildStrategy(String.join("\n", includeRegions));
        assertTrue(includeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    @Test
    public void testIncludeIsReturnsFalseIfNoOneAffectedFileInTheRange() throws Exception {
        String commits = generateCommitLogString(".gitignore","README.md");
        setupGitSourceMocksWithCommits(commits);
        IncludeRegionBranchBuildStrategy includeRegionBranchBuildStrategy = new IncludeRegionBranchBuildStrategy(String.join("\n", includeRegions));
        assertFalse(includeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    @Test
    public void testIncludeIsReturnsFalseIfIncludedRangeIsEmpty() throws Exception {
        String commits = generateCommitLogString(".gitignore","README.md");
        setupGitSourceMocksWithCommits(commits);
        IncludeRegionBranchBuildStrategy includeRegionBranchBuildStrategy = new IncludeRegionBranchBuildStrategy("");
        assertFalse(includeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    
    
    
    
    @Test
    public void testExclludeIsReturnsFalseIfAllAffectedFileInTheRange() throws Exception {
        String commits = generateCommitLogString(".gitignore","README.md") + generateCommitLogString(".gitignore","README2.md");
        setupGitSourceMocksWithCommits(commits);
        ExcludeRegionBranchBuildStrategy excludeRegionBranchBuildStrategy = new ExcludeRegionBranchBuildStrategy(String.join("\n", excludeRegions));
        assertFalse(excludeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    
    @Test
    public void testExclludeIsReturnsTrueIfAllAffectedFileInTheRange() throws Exception {
        String commits = generateCommitLogString(".gitignore","README.md") + generateCommitLogString(".gitignore","README.md2");
        setupGitSourceMocksWithCommits(commits);
        ExcludeRegionBranchBuildStrategy excludeRegionBranchBuildStrategy = new ExcludeRegionBranchBuildStrategy(String.join("\n", excludeRegions));
        assertTrue(excludeRegionBranchBuildStrategy.isAutomaticBuild(source, head, currRevision, prevRevision));
    }
    
    
    
    
    
    
    private void setupGitSourceMocksWithCommits(String commits) throws Exception {
    	SCMSourceOwner ownerMock = PowerMockito.mock(WorkflowMultiBranchProject.class);
    	GitSCMFileSystem.BuilderImpl builderMock = Mockito.mock(GitSCMFileSystem.BuilderImpl.class);
    	ByteArrayOutputStream ByteArrayOutputStreamMock = Mockito.mock(ByteArrayOutputStream.class);
        GitSCMFileSystem fileSystemMock = Mockito.mock(GitSCMFileSystem.class);
        PowerMockito.when(source.build(head, currRevision)).thenReturn(scm);
        PowerMockito.when(source.getOwner()).thenReturn(ownerMock);        
        Mockito.when(ByteArrayOutputStreamMock.toByteArray()).thenReturn(commits.getBytes());
        Mockito.when(builderMock.build(source.getOwner(), scm, currRevision)).thenReturn(fileSystemMock);
        Mockito.when(fileSystemMock.changesSince(prevRevision, ByteArrayOutputStreamMock)).thenReturn(true);
        PowerMockito.whenNew(ByteArrayOutputStream.class).withNoArguments().thenReturn(ByteArrayOutputStreamMock);
        PowerMockito.whenNew(GitSCMFileSystem.BuilderImpl.class).withNoArguments().thenReturn(builderMock);
    }
      
    
    private String randomHash() {
    	return  new BigInteger(168, new Random()).toString(16).substring(0,40);
    }
    
    
    private String generateCommitLogString(String...affectedFiles){
    	
    	List<String> lines = new ArrayList<String>();
    	lines.add("commit " + randomHash());
    	lines.add(getDefaultAuthorLine());
    	lines.add(getDefaultCommentLine());
    	for (String affectedFile : affectedFiles)
    		lines.add(getAffectedFileLine(affectedFile));
    	lines.add("\n");
    	return String.join("\n", lines);
    }
    
    
    
    private String getDefaultAuthorLine() {
		return "author Test Test<test@test.com>";
	}

    
    private String getDefaultCommentLine() {
		return "    comment";
	}
    
    private String getAffectedFileLine(String affectedFilePath) {
    	return ":100644 100644 " + randomHash() + " " + randomHash() + " M\t" + affectedFilePath;
    }

	
	
}